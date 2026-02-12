package github.lms.lemuel.dto;

import github.lms.lemuel.domain.Settlement;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class SettlementResponse {

    private Long id;
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private LocalDate settlementDate;
    private LocalDateTime confirmedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SettlementResponse(Settlement settlement) {
        this.id = settlement.getId();
        this.paymentId = settlement.getPaymentId();
        this.orderId = settlement.getOrderId();
        this.amount = settlement.getAmount();
        this.status = settlement.getStatus().name();
        this.settlementDate = settlement.getSettlementDate();
        this.confirmedAt = settlement.getConfirmedAt();
        this.approvedBy = settlement.getApprovedBy();
        this.approvedAt = settlement.getApprovedAt();
        this.rejectedBy = settlement.getRejectedBy();
        this.rejectedAt = settlement.getRejectedAt();
        this.rejectionReason = settlement.getRejectionReason();
        this.createdAt = settlement.getCreatedAt();
        this.updatedAt = settlement.getUpdatedAt();
    }
}
