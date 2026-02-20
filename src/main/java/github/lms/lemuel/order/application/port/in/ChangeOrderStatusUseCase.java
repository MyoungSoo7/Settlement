package github.lms.lemuel.order.application.port.in;

import github.lms.lemuel.order.domain.Order;

/**
 * 주문 상태 변경 UseCase (Inbound Port)
 */
public interface ChangeOrderStatusUseCase {

    Order cancelOrder(Long orderId);
}
