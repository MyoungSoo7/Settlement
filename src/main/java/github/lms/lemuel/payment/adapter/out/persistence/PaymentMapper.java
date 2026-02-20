package github.lms.lemuel.payment.adapter.out.persistence;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.PaymentStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper between Domain Payment and JPA PaymentJpaEntity
 */
@Component
public class PaymentMapper {

    /**
     * Map JPA entity to domain model
     */
    public PaymentDomain toDomain(PaymentJpaEntity entity) {
        return new PaymentDomain(
            entity.getId(),
            entity.getOrderId(),
            entity.getAmount(),
            entity.getRefundedAmount(),
            PaymentStatus.valueOf(entity.getStatus()),
            entity.getPaymentMethod(),
            entity.getPgTransactionId(),
            entity.getCapturedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Map domain model to JPA entity
     */
    public PaymentJpaEntity toJpaEntity(PaymentDomain paymentDomain) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(paymentDomain.getId());
        entity.setOrderId(paymentDomain.getOrderId());
        entity.setAmount(paymentDomain.getAmount());
        entity.setRefundedAmount(paymentDomain.getRefundedAmount());
        entity.setStatus(paymentDomain.getStatus().name());
        entity.setPaymentMethod(paymentDomain.getPaymentMethod());
        entity.setPgTransactionId(paymentDomain.getPgTransactionId());
        entity.setCapturedAt(paymentDomain.getCapturedAt());
        entity.setCreatedAt(paymentDomain.getCreatedAt());
        entity.setUpdatedAt(paymentDomain.getUpdatedAt());
        return entity;
    }
}
