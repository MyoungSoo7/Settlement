package github.lms.lemuel.order.application.port.out;

import github.lms.lemuel.order.domain.Order;

/**
 * 주문 저장 Outbound Port
 */
public interface SaveOrderPort {

    Order save(Order order);
}
