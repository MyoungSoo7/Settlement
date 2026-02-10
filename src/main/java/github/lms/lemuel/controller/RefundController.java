package github.lms.lemuel.controller;

import github.lms.lemuel.domain.Payment;
import github.lms.lemuel.domain.Refund;
import github.lms.lemuel.dto.PaymentResponse;
import github.lms.lemuel.dto.RefundRequest;
import github.lms.lemuel.dto.RefundResponse;
import github.lms.lemuel.exception.MissingIdempotencyKeyException;
import github.lms.lemuel.repository.PaymentRepository;
import github.lms.lemuel.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/refunds")
public class RefundController {

    private final RefundService refundService;
    private final PaymentRepository paymentRepository;

    public RefundController(RefundService refundService, PaymentRepository paymentRepository) {
        this.refundService = refundService;
        this.paymentRepository = paymentRepository;
    }

    /**
     * 환불 요청 (부분/전체 환불 통합)
     */
    @PostMapping("/{paymentId}")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable Long paymentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RefundRequest request) {

        // Idempotency-Key 필수 체크
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("Idempotency-Key 헤더는 필수입니다.");
        }

        Refund refund = refundService.createRefund(
                paymentId,
                request.getAmount(),
                idempotencyKey,
                request.getReason()
        );

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        return ResponseEntity.ok(new RefundResponse(refund, payment));
    }

    /**
     * 시나리오 1: 전체 환불 (기존 API 호환)
     */
    @PostMapping("/full/{paymentId}")
    public ResponseEntity<PaymentResponse> processFullRefund(
            @PathVariable Long paymentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("Idempotency-Key 헤더는 필수입니다.");
        }

        Payment refundedPayment = refundService.processFullRefund(paymentId, idempotencyKey);
        return ResponseEntity.ok(new PaymentResponse(refundedPayment));
    }

    /**
     * 시나리오 2: 부분 환불 (기존 API 호환)
     */
    @PostMapping("/partial/{paymentId}")
    public ResponseEntity<PaymentResponse> processPartialRefund(
            @PathVariable Long paymentId,
            @RequestParam BigDecimal refundAmount,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("Idempotency-Key 헤더는 필수입니다.");
        }

        Payment refundPayment = refundService.processPartialRefund(paymentId, refundAmount, idempotencyKey);
        return ResponseEntity.ok(new PaymentResponse(refundPayment));
    }

    /**
     * 시나리오 3: 결제 실패 환불 (취소)
     */
    @PostMapping("/failed/{paymentId}")
    public ResponseEntity<PaymentResponse> processFailedPaymentRefund(@PathVariable Long paymentId) {
        Payment canceledPayment = refundService.processFailedPaymentRefund(paymentId);
        return ResponseEntity.ok(new PaymentResponse(canceledPayment));
    }
}
