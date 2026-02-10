package github.lms.lemuel.service;

import github.lms.lemuel.domain.Payment;
import github.lms.lemuel.domain.Refund;
import github.lms.lemuel.exception.InvalidPaymentStateException;
import github.lms.lemuel.exception.RefundExceedsPaymentException;
import github.lms.lemuel.repository.PaymentRepository;
import github.lms.lemuel.repository.RefundRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 환불 처리 서비스 (리팩토링 버전)
 *
 * 핵심 원칙:
 * 1. Payment는 원 결제를 대표하는 단일 레코드 (음수 레코드 생성 금지)
 * 2. 환불은 Refund 엔티티로 분리하여 관리
 * 3. 멱등성 보장: Idempotency-Key 기반
 * 4. 동시성 제어: Payment row-level lock (PESSIMISTIC_WRITE)
 * 5. 환불 누적 합계는 Payment.refundedAmount에 저장
 */
@Service
public class RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SettlementAdjustmentService adjustmentService;
    private final EntityManager entityManager;

    public RefundService(PaymentRepository paymentRepository,
                         RefundRepository refundRepository,
                         SettlementAdjustmentService adjustmentService,
                         EntityManager entityManager) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.adjustmentService = adjustmentService;
        this.entityManager = entityManager;
    }

    /**
     * 환불 생성 (부분/전체 환불 통합)
     *
     * @param paymentId 결제 ID
     * @param refundAmount 환불 금액
     * @param idempotencyKey 멱등성 키
     * @param reason 환불 사유
     * @return 생성/조회된 환불 레코드
     */
    @Transactional
    public Refund createRefund(Long paymentId, BigDecimal refundAmount, String idempotencyKey, String reason) {
        logger.info("환불 처리 시작: paymentId={}, amount={}, idempotencyKey={}", paymentId, refundAmount, idempotencyKey);

        // 1. 멱등성 체크: 동일 키로 이미 환불 요청이 있으면 재사용
        Optional<Refund> existingRefund = refundRepository.findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey);
        if (existingRefund.isPresent()) {
            logger.info("멱등성 키 재사용: refundId={}, idempotencyKey={}", existingRefund.get().getId(), idempotencyKey);
            return existingRefund.get();
        }

        // 2. Payment row-level lock 획득 (동시성 제어)
        Payment payment = entityManager.find(Payment.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (payment == null) {
            throw new RuntimeException("Payment not found: " + paymentId);
        }

        // 3. 유효성 검사
        validateRefundRequest(payment, refundAmount);

        // 4. Refund 레코드 생성
        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(refundAmount);
        refund.setStatus(Refund.RefundStatus.REQUESTED);
        refund.setIdempotencyKey(idempotencyKey);
        refund.setReason(reason);
        refundRepository.save(refund);

        // 5. 환불 완료 처리 (실제 PG 연동 시에는 비동기 처리)
        completeRefund(refund, payment);

        logger.info("환불 처리 완료: refundId={}, paymentId={}, amount={}", refund.getId(), paymentId, refundAmount);
        return refund;
    }

    /**
     * 환불 완료 처리
     */
    @Transactional
    public void completeRefund(Refund refund, Payment payment) {
        // Refund 상태 업데이트
        refund.setStatus(Refund.RefundStatus.COMPLETED);
        refundRepository.save(refund);

        // Payment 환불 누적 금액 업데이트
        BigDecimal newRefundedAmount = payment.getRefundedAmount().add(refund.getAmount());
        payment.setRefundedAmount(newRefundedAmount);

        // 전액 환불 시 Payment 상태 변경
        if (payment.isFullyRefunded()) {
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            logger.info("전액 환불 완료: paymentId={}", payment.getId());
        }

        paymentRepository.save(payment);

        // 정산 조정 처리
        adjustmentService.createAdjustmentForRefund(refund);
    }

    /**
     * 환불 요청 유효성 검사
     */
    private void validateRefundRequest(Payment payment, BigDecimal refundAmount) {
        // 상태 검사
        if (payment.getStatus() != Payment.PaymentStatus.CAPTURED) {
            throw new InvalidPaymentStateException(
                    String.format("CAPTURED 상태의 결제만 환불 가능합니다. 현재 상태: %s", payment.getStatus()));
        }

        // 금액 검사
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }

        // 초과 환불 방지
        BigDecimal refundableAmount = payment.getRefundableAmount();
        if (refundAmount.compareTo(refundableAmount) > 0) {
            throw new RefundExceedsPaymentException(
                    String.format("환불 가능 금액을 초과했습니다. 환불 가능: %s, 요청: %s",
                            refundableAmount, refundAmount));
        }
    }

    /**
     * 전체 환불 (기존 API 호환)
     */
    @Transactional
    public Payment processFullRefund(Long paymentId, String idempotencyKey) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        BigDecimal refundableAmount = payment.getRefundableAmount();
        createRefund(paymentId, refundableAmount, idempotencyKey, "전체 환불");

        return paymentRepository.findById(paymentId).orElseThrow();
    }

    /**
     * 부분 환불 (기존 API 호환)
     */
    @Transactional
    public Payment processPartialRefund(Long paymentId, BigDecimal refundAmount, String idempotencyKey) {
        createRefund(paymentId, refundAmount, idempotencyKey, "부분 환불");
        return paymentRepository.findById(paymentId).orElseThrow();
    }

    /**
     * 결제 실패 환불 (취소)
     */
    @Transactional
    public Payment processFailedPaymentRefund(Long paymentId) {
        logger.info("결제 실패 환불(취소) 처리 시작: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        // 유효성 검사: AUTHORIZED 또는 FAILED 상태만 취소 가능
        if (payment.getStatus() != Payment.PaymentStatus.AUTHORIZED
                && payment.getStatus() != Payment.PaymentStatus.FAILED) {
            throw new InvalidPaymentStateException(
                    "AUTHORIZED 또는 FAILED 상태의 결제만 취소 가능합니다. 현재 상태: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.CANCELED);
        paymentRepository.save(payment);

        logger.info("결제 실패 환불(취소) 처리 완료: paymentId={}", paymentId);
        return payment;
    }
}
