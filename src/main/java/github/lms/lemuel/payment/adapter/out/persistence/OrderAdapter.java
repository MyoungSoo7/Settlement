package github.lms.lemuel.payment.adapter.out.persistence;

import github.lms.lemuel.order.adapter.out.persistence.OrderJpaEntity;
import github.lms.lemuel.order.adapter.out.persistence.OrderPersistenceMapper;
import github.lms.lemuel.order.adapter.out.persistence.SpringDataOrderJpaRepository;
import github.lms.lemuel.order.domain.Order;
import github.lms.lemuel.order.domain.OrderStatus;
import github.lms.lemuel.payment.application.port.out.LoadOrderPort;
import github.lms.lemuel.payment.application.port.out.LoadOrderPort.OrderInfo;
import github.lms.lemuel.payment.application.port.out.UpdateOrderStatusPort;
import org.springframework.stereotype.Component;

/**
 * Adapter for accessing Order aggregate from Payment bounded context
 * Payment bounded-context에서 Order 조회/수정을 위한 어댑터
 */
@Component
public class OrderAdapter implements LoadOrderPort, UpdateOrderStatusPort {

    private final SpringDataOrderJpaRepository orderRepository;
    private final OrderPersistenceMapper orderMapper;

    public OrderAdapter(SpringDataOrderJpaRepository orderRepository,
                        OrderPersistenceMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public OrderInfo loadOrder(Long orderId) {
        OrderJpaEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        Order order = orderMapper.toDomain(orderEntity);

        return new OrderInfo(
                order.getId(),
                order.getAmount(),
                order.getStatus().name()
        );
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        OrderJpaEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        Order order = orderMapper.toDomain(orderEntity);
        order.setStatus(OrderStatus.valueOf(status));

        OrderJpaEntity updatedEntity = orderMapper.toEntity(order);
        orderRepository.save(updatedEntity);
    }
}
