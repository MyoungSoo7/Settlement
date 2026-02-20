package github.lms.lemuel.order.application.service;

import github.lms.lemuel.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import github.lms.lemuel.order.application.port.out.LoadUserForOrderPort;
import github.lms.lemuel.order.application.port.out.SaveOrderPort;
import github.lms.lemuel.order.domain.Order;
import github.lms.lemuel.order.domain.OrderStatus;
import github.lms.lemuel.order.domain.exception.UserNotExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderService 단위 테스트")
class CreateOrderServiceTest {

    @Mock
    private LoadUserForOrderPort loadUserForOrderPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @InjectMocks
    private CreateOrderService createOrderService;

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("10000.00");
        CreateOrderCommand command = new CreateOrderCommand(userId, amount);

        given(loadUserForOrderPort.existsUser(userId)).willReturn(true);

        Order savedOrder = new Order(1L, userId, amount, OrderStatus.CREATED,
                LocalDateTime.now(), LocalDateTime.now());
        given(saveOrderPort.save(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = createOrderService.createOrder(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);

        verify(loadUserForOrderPort).existsUser(userId);
        verify(saveOrderPort).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 사용자 존재하지 않음")
    void createOrder_UserNotExists_ThrowsException() {
        // given
        Long userId = 999L;
        CreateOrderCommand command = new CreateOrderCommand(userId, new BigDecimal("10000.00"));

        given(loadUserForOrderPort.existsUser(userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> createOrderService.createOrder(command))
                .isInstanceOf(UserNotExistsException.class)
                .hasMessageContaining(userId.toString());

        verify(loadUserForOrderPort).existsUser(userId);
        verify(saveOrderPort, never()).save(any(Order.class));
    }
}
