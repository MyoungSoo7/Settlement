package github.lms.lemuel.service;

import github.lms.lemuel.domain.Payment;
import github.lms.lemuel.domain.Refund;
import github.lms.lemuel.domain.Settlement;
import github.lms.lemuel.domain.SettlementAdjustment;
import github.lms.lemuel.exception.InvalidPaymentStateException;
import github.lms.lemuel.exception.RefundExceedsPaymentException;
import github.lms.lemuel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * RefundService 통합 테스트
 *
 * 테스트 시나리오:
 * 1. 부분 환불 2회 누적
 * 2. 초과 환불 시도 시 실패
 * 3. 멱등성 키 재사용
 * 4. CONFIRMED 정산 후 환불 시 조정 생성
 */
@SpringBootTest
@Transactional
class RefundServiceIntegrationTest {

    @Autowired
    private RefundService refundService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private SettlementAdjustmentRepository adjustmentRepository;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        // 테스트용 CAPTURED 결제 생성
        testPayment = new Payment();
        testPayment.setOrderId(1L);
        testPayment.setAmount(new BigDecimal("10000.00"));
        testPayment.setStatus(Payment.PaymentStatus.CAPTURED);
        testPayment.setPaymentMethod("CARD");
        testPayment.setPgTransactionId("PG-TEST-" + UUID.randomUUID());
        testPayment = paymentRepository.save(testPayment);
    }

    @Test
    @DisplayName("시나리오 1: 부분환불 2회 누적 - refunded_amount 10000, status REFUNDED")
    void testPartialRefundTwice() {
        // 첫 번째 부분 환불: 3000원
        String idempotencyKey1 = UUID.randomUUID().toString();
        Refund refund1 = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("3000.00"),
                idempotencyKey1,
                "부분환불 1차"
        );

        assertThat(refund1.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);
        assertThat(refund1.getAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));

        // Payment 확인: refundedAmount=3000, status=CAPTURED
        Payment afterFirst = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(afterFirst.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(afterFirst.getStatus()).isEqualTo(Payment.PaymentStatus.CAPTURED);
        assertThat(afterFirst.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("7000.00"));

        // 두 번째 부분 환불: 7000원 (전액)
        String idempotencyKey2 = UUID.randomUUID().toString();
        Refund refund2 = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("7000.00"),
                idempotencyKey2,
                "부분환불 2차"
        );

        assertThat(refund2.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);

        // Payment 확인: refundedAmount=10000, status=REFUNDED
        Payment afterSecond = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(afterSecond.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(afterSecond.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(afterSecond.isFullyRefunded()).isTrue();
    }

    @Test
    @DisplayName("시나리오 2: 초과환불 시도 시 409 Conflict")
    void testRefundExceedsPaymentAmount() {
        // 10000원 결제에 10001원 환불 시도
        String idempotencyKey = UUID.randomUUID().toString();

        assertThatThrownBy(() -> refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("10001.00"),
                idempotencyKey,
                "초과환불 시도"
        ))
                .isInstanceOf(RefundExceedsPaymentException.class)
                .hasMessageContaining("환불 가능 금액을 초과");
    }

    @Test
    @DisplayName("시나리오 3: 동일 Idempotency-Key 재요청 시 동일 Refund 반환")
    void testIdempotencyKeyReuse() {
        String idempotencyKey = UUID.randomUUID().toString();

        // 첫 번째 요청
        Refund refund1 = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("5000.00"),
                idempotencyKey,
                "멱등성 테스트"
        );

        // 동일 키로 두 번째 요청
        Refund refund2 = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("5000.00"),
                idempotencyKey,
                "멱등성 테스트"
        );

        // 동일한 Refund 레코드 반환
        assertThat(refund1.getId()).isEqualTo(refund2.getId());
        assertThat(refund1.getIdempotencyKey()).isEqualTo(idempotencyKey);

        // Payment의 refundedAmount는 1회만 반영
        Payment payment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("시나리오 4: CONFIRMED 정산 후 환불 시 SettlementAdjustment 생성")
    void testRefundAfterConfirmedSettlement() {
        // Settlement CONFIRMED 상태 생성
        Settlement settlement = new Settlement();
        settlement.setPaymentId(testPayment.getId());
        settlement.setOrderId(testPayment.getOrderId());
        settlement.setAmount(testPayment.getAmount());
        settlement.setStatus(Settlement.SettlementStatus.CONFIRMED);
        settlement.setSettlementDate(LocalDate.now().minusDays(1));
        settlement = settlementRepository.save(settlement);

        // 환불 실행
        String idempotencyKey = UUID.randomUUID().toString();
        Refund refund = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("2000.00"),
                idempotencyKey,
                "정산 확정 후 환불"
        );

        assertThat(refund.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);

        // SettlementAdjustment 생성 확인
        SettlementAdjustment adjustment = adjustmentRepository.findByRefundId(refund.getId()).orElseThrow();
        assertThat(adjustment.getSettlementId()).isEqualTo(settlement.getId());
        assertThat(adjustment.getRefundId()).isEqualTo(refund.getId());
        assertThat(adjustment.getAmount()).isEqualByComparingTo(new BigDecimal("-2000.00"));
        assertThat(adjustment.getStatus()).isEqualTo(SettlementAdjustment.AdjustmentStatus.PENDING);
    }

    @Test
    @DisplayName("시나리오 5: PENDING 정산 후 환불 시 Settlement 금액 직접 차감")
    void testRefundAfterPendingSettlement() {
        // Settlement PENDING 상태 생성
        Settlement settlement = new Settlement();
        settlement.setPaymentId(testPayment.getId());
        settlement.setOrderId(testPayment.getOrderId());
        settlement.setAmount(testPayment.getAmount());
        settlement.setStatus(Settlement.SettlementStatus.PENDING);
        settlement.setSettlementDate(LocalDate.now());
        settlement = settlementRepository.save(settlement);

        // 환불 실행
        String idempotencyKey = UUID.randomUUID().toString();
        Refund refund = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("3000.00"),
                idempotencyKey,
                "정산 확정 전 환불"
        );

        assertThat(refund.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);

        // Settlement 금액 직접 차감 확인
        Settlement updatedSettlement = settlementRepository.findById(settlement.getId()).orElseThrow();
        assertThat(updatedSettlement.getAmount()).isEqualByComparingTo(new BigDecimal("7000.00"));

        // SettlementAdjustment는 생성되지 않음
        assertThat(adjustmentRepository.findByRefundId(refund.getId())).isEmpty();
    }

    @Test
    @DisplayName("시나리오 6: READY 상태 결제는 환불 불가 (InvalidPaymentStateException)")
    void testRefundFailsForNonCapturedPayment() {
        // READY 상태 결제 생성
        Payment readyPayment = new Payment();
        readyPayment.setOrderId(2L);
        readyPayment.setAmount(new BigDecimal("5000.00"));
        readyPayment.setStatus(Payment.PaymentStatus.READY);
        readyPayment = paymentRepository.save(readyPayment);

        String idempotencyKey = UUID.randomUUID().toString();
        Long paymentId = readyPayment.getId();

        assertThatThrownBy(() -> refundService.createRefund(
                paymentId,
                new BigDecimal("1000.00"),
                idempotencyKey,
                "잘못된 상태 환불 시도"
        ))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("CAPTURED 상태의 결제만 환불 가능");
    }

    // ===== 경계값 테스트 추가 =====

    @Test
    @DisplayName("경계값 1: 정확히 결제 금액과 동일한 환불 (전액 환불)")
    void testExactAmountRefund() {
        String idempotencyKey = UUID.randomUUID().toString();

        // 10000원 결제에 정확히 10000원 환불
        Refund refund = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("10000.00"),
                idempotencyKey,
                "정확히 전액 환불"
        );

        assertThat(refund.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);
        assertThat(refund.getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));

        Payment payment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(payment.isFullyRefunded()).isTrue();
        assertThat(payment.getRefundableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("경계값 2: 잔액이 딱 1원일 때 1원 환불")
    void testOneWonRefundWhenOneWonLeft() {
        // 9999원 먼저 환불
        String idempotencyKey1 = UUID.randomUUID().toString();
        refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("9999.00"),
                idempotencyKey1,
                "9999원 환불"
        );

        Payment afterFirst = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(afterFirst.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(afterFirst.getStatus()).isEqualTo(Payment.PaymentStatus.CAPTURED);

        // 남은 1원 환불
        String idempotencyKey2 = UUID.randomUUID().toString();
        Refund refund2 = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("1.00"),
                idempotencyKey2,
                "마지막 1원 환불"
        );

        assertThat(refund2.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);

        Payment afterSecond = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(afterSecond.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(afterSecond.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(afterSecond.getRefundableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("경계값 3: 잔액이 1원 부족한 환불 시도 (실패)")
    void testRefundExceedsBy1Won() {
        // 9999원 먼저 환불
        String idempotencyKey1 = UUID.randomUUID().toString();
        refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("9999.00"),
                idempotencyKey1,
                "9999원 환불"
        );

        // 잔액 1원인데 2원 환불 시도
        String idempotencyKey2 = UUID.randomUUID().toString();
        assertThatThrownBy(() -> refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("2.00"),
                idempotencyKey2,
                "초과 환불 시도"
        ))
                .isInstanceOf(RefundExceedsPaymentException.class)
                .hasMessageContaining("환불 가능 금액을 초과했습니다. 환불 가능: 1.00, 요청: 2.00");

        // Payment 상태는 변경되지 않음
        Payment payment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("9999.00"));
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.CAPTURED);
    }

    @Test
    @DisplayName("경계값 4: 0원 환불 시도 (실패)")
    void testZeroAmountRefund() {
        String idempotencyKey = UUID.randomUUID().toString();

        assertThatThrownBy(() -> refundService.createRefund(
                testPayment.getId(),
                BigDecimal.ZERO,
                idempotencyKey,
                "0원 환불 시도"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환불 금액은 0보다 커야 합니다");

        // Payment 상태는 변경되지 않음
        Payment payment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.CAPTURED);
    }

    @Test
    @DisplayName("경계값 5: 음수 금액 환불 시도 (실패)")
    void testNegativeAmountRefund() {
        String idempotencyKey = UUID.randomUUID().toString();

        assertThatThrownBy(() -> refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("-1000.00"),
                idempotencyKey,
                "음수 환불 시도"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환불 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("경계값 6: 소수점 정밀도 테스트 (0.01원)")
    void testDecimalPrecisionRefund() {
        // 0.01원 환불
        String idempotencyKey1 = UUID.randomUUID().toString();
        Refund refund1 = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("0.01"),
                idempotencyKey1,
                "0.01원 환불"
        );

        assertThat(refund1.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);

        Payment afterFirst = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(afterFirst.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(afterFirst.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("9999.99"));

        // 9999.99원 환불 (전액)
        String idempotencyKey2 = UUID.randomUUID().toString();
        Refund refund2 = refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("9999.99"),
                idempotencyKey2,
                "나머지 전액 환불"
        );

        assertThat(refund2.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);

        Payment afterSecond = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(afterSecond.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(afterSecond.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("경계값 7: 부분환불 3회 누적 (복잡한 시나리오)")
    void testMultiplePartialRefunds() {
        // 1차: 3333.33원
        String key1 = UUID.randomUUID().toString();
        refundService.createRefund(testPayment.getId(), new BigDecimal("3333.33"), key1, "1차");

        Payment after1 = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(after1.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("3333.33"));
        assertThat(after1.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("6666.67"));
        assertThat(after1.getStatus()).isEqualTo(Payment.PaymentStatus.CAPTURED);

        // 2차: 3333.33원
        String key2 = UUID.randomUUID().toString();
        refundService.createRefund(testPayment.getId(), new BigDecimal("3333.33"), key2, "2차");

        Payment after2 = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(after2.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("6666.66"));
        assertThat(after2.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("3333.34"));
        assertThat(after2.getStatus()).isEqualTo(Payment.PaymentStatus.CAPTURED);

        // 3차: 3333.34원 (전액)
        String key3 = UUID.randomUUID().toString();
        refundService.createRefund(testPayment.getId(), new BigDecimal("3333.34"), key3, "3차");

        Payment after3 = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(after3.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(after3.getRefundableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(after3.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(after3.isFullyRefunded()).isTrue();
    }

    @Test
    @DisplayName("경계값 8: 전액 환불 후 추가 환불 시도 (실패)")
    void testRefundAfterFullRefund() {
        // 전액 환불
        String key1 = UUID.randomUUID().toString();
        refundService.createRefund(testPayment.getId(), new BigDecimal("10000.00"), key1, "전액 환불");

        Payment payment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);

        // 추가 환불 시도 (REFUNDED 상태)
        String key2 = UUID.randomUUID().toString();
        assertThatThrownBy(() -> refundService.createRefund(
                testPayment.getId(),
                new BigDecimal("1.00"),
                key2,
                "추가 환불 시도"
        ))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("CAPTURED 상태의 결제만 환불 가능");
    }

    @Test
    @DisplayName("경계값 9: 매우 큰 금액 환불 (오버플로우 방지)")
    void testLargeAmountRefund() {
        // 큰 금액 결제 생성
        Payment largePayment = new Payment();
        largePayment.setOrderId(999L);
        largePayment.setAmount(new BigDecimal("99999999.99")); // DECIMAL(10, 2) 최대값
        largePayment.setStatus(Payment.PaymentStatus.CAPTURED);
        largePayment.setPaymentMethod("CARD");
        largePayment.setPgTransactionId("PG-LARGE-" + UUID.randomUUID());
        largePayment = paymentRepository.save(largePayment);

        // 전액 환불
        String idempotencyKey = UUID.randomUUID().toString();
        Refund refund = refundService.createRefund(
                largePayment.getId(),
                new BigDecimal("99999999.99"),
                idempotencyKey,
                "큰 금액 환불"
        );

        assertThat(refund.getStatus()).isEqualTo(Refund.RefundStatus.COMPLETED);

        Payment afterRefund = paymentRepository.findById(largePayment.getId()).orElseThrow();
        assertThat(afterRefund.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("99999999.99"));
        assertThat(afterRefund.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
    }
}
