package github.lms.lemuel.order.adapter.out.persistence;

import github.lms.lemuel.order.domain.Order;
import github.lms.lemuel.order.domain.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * Domain <-> JpaEntity 매핑
 */
@Component
public class OrderPersistenceMapper {

    public Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Order(
                entity.getId(),
                entity.getUserId(),
                entity.getAmount(),
                OrderStatus.fromString(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OrderJpaEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }

        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setAmount(domain.getAmount());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
