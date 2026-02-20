package github.lms.lemuel.order.application.service;

import github.lms.lemuel.order.application.port.in.CreateOrderUseCase;
import github.lms.lemuel.order.application.port.out.LoadUserForOrderPort;
import github.lms.lemuel.order.application.port.out.SaveOrderPort;
import github.lms.lemuel.order.domain.Order;
import github.lms.lemuel.order.domain.exception.UserNotExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final LoadUserForOrderPort loadUserForOrderPort;
    private final SaveOrderPort saveOrderPort;

    @Override
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        // 사용자 존재 검증
        if (!loadUserForOrderPort.existsUser(command.userId())) {
            throw new UserNotExistsException(command.userId());
        }

        // 도메인 생성
        Order order = Order.create(command.userId(), command.amount());

        // 저장
        return saveOrderPort.save(order);
    }
}
