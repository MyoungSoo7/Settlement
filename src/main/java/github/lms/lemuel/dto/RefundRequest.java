package github.lms.lemuel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RefundRequest {

    @NotNull(message = "환불 금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "환불 금액은 0보다 커야 합니다")
    private BigDecimal amount;

    private String reason;

    public RefundRequest() {
    }

    public RefundRequest(BigDecimal amount, String reason) {
        this.amount = amount;
        this.reason = reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
