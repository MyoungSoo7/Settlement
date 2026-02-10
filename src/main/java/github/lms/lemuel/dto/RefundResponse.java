package github.lms.lemuel.dto;

import github.lms.lemuel.domain.Payment;
import github.lms.lemuel.domain.Refund;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponse {

    private Long refundId;
    private Long paymentId;
    private BigDecimal refundAmount;
    private String refundStatus;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;

    // Payment 정보
    private BigDecimal paymentAmount;
    private BigDecimal refundedAmount;
    private BigDecimal refundableAmount;
    private String paymentStatus;

    public RefundResponse(Refund refund, Payment payment) {
        this.refundId = refund.getId();
        this.paymentId = refund.getPaymentId();
        this.refundAmount = refund.getAmount();
        this.refundStatus = refund.getStatus().name();
        this.reason = refund.getReason();
        this.requestedAt = refund.getRequestedAt();
        this.completedAt = refund.getCompletedAt();

        this.paymentAmount = payment.getAmount();
        this.refundedAmount = payment.getRefundedAmount();
        this.refundableAmount = payment.getRefundableAmount();
        this.paymentStatus = payment.getStatus().name();
    }

    // Getters
    public Long getRefundId() {
        return refundId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public BigDecimal getRefundableAmount() {
        return refundableAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }
}
