package github.lms.lemuel.settlement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Settlement Domain Entity (순수 POJO, 스프링/JPA 의존성 없음)
 * DB 스키마: id, payment_id, order_id, amount, status, settlement_date, confirmed_at, created_at, updated_at
 */
public class Settlement {

    private Long id;
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private SettlementStatus status;
    private LocalDate settlementDate;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Settlement() {
        this.status = SettlementStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 전체 생성자
    public Settlement(Long id, Long paymentId, Long orderId, BigDecimal amount,
                      SettlementStatus status, LocalDate settlementDate, LocalDateTime confirmedAt,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status != null ? status : SettlementStatus.PENDING;
        this.settlementDate = settlementDate;
        this.confirmedAt = confirmedAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    // 정적 팩토리 메서드
    public static Settlement create(Long paymentId, Long orderId, BigDecimal amount, LocalDate settlementDate) {
        Settlement settlement = new Settlement();
        settlement.setPaymentId(paymentId);
        settlement.setOrderId(orderId);
        settlement.setAmount(amount);
        settlement.setSettlementDate(settlementDate);
        settlement.validatePaymentId();
        settlement.validateAmount();
        settlement.validateSettlementDate();
        return settlement;
    }

    // 도메인 규칙: paymentId 검증
    public void validatePaymentId() {
        if (paymentId == null || paymentId <= 0) {
            throw new IllegalArgumentException("Payment ID must be a positive number");
        }
    }

    // 도메인 규칙: amount 검증
    public void validateAmount() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    // 도메인 규칙: settlementDate 검증
    public void validateSettlementDate() {
        if (settlementDate == null) {
            throw new IllegalArgumentException("Settlement date is required");
        }
    }

    // 비즈니스 메서드: 정산 확정
    public void confirm() {
        if (this.status != SettlementStatus.PENDING && this.status != SettlementStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("Only PENDING or WAITING_APPROVAL settlements can be confirmed");
        }
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드: 정산 취소
    public void cancel() {
        if (this.status == SettlementStatus.CONFIRMED) {
            throw new IllegalStateException("CONFIRMED settlements cannot be canceled");
        }
        this.status = SettlementStatus.CANCELED;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 쿼리 메서드
    public boolean isConfirmed() {
        return this.status == SettlementStatus.CONFIRMED;
    }

    public boolean isPending() {
        return this.status == SettlementStatus.PENDING || this.status == SettlementStatus.WAITING_APPROVAL;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
