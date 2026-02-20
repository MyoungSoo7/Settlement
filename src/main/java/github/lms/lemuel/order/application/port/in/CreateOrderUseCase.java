package github.lms.lemuel.order.application.port.in;

import github.lms.lemuel.order.domain.Order;

import java.math.BigDecimal;

/**
 * 주문 생성 UseCase (Inbound Port)
 */
public interface CreateOrderUseCase {

    Order createOrder(CreateOrderCommand command);

    record CreateOrderCommand(Long userId, BigDecimal amount) {
        public CreateOrderCommand {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("User ID is required and must be positive");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }
        }
    }
}
