package github.lms.lemuel.order.application.service;

import github.lms.lemuel.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import github.lms.lemuel.order.application.port.out.LoadUserForOrderPort;
import github.lms.lemuel.order.application.port.out.SaveOrderPort;
import github.lms.lemuel.order.application.port.out.SendOrderNotificationPort;
import github.lms.lemuel.order.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderService - 주문 생성 시 이메일 알림 발송")
class CreateOrderServiceTest {

    @Mock
    private LoadUserForOrderPort loadUserForOrderPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @Mock
    private SendOrderNotificationPort sendOrderNotificationPort;

    @InjectMocks
    private CreateOrderService createOrderService;

    @Test
    @DisplayName("주문 생성이 성공하면 사용자 이메일로 확인 메일을 발송한다")
    void createOrder_SendsEmailNotification() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        BigDecimal amount = new BigDecimal("12345.67");
        String userEmail = "seed_user1@test.com";

        CreateOrderCommand command = new CreateOrderCommand(userId, productId, amount);

        given(loadUserForOrderPort.findEmailById(userId)).willReturn(Optional.of(userEmail));
        given(saveOrderPort.save(any(Order.class))).willAnswer(invocation -> {
            Order o = invocation.getArgument(0, Order.class);
            o.setId(100L);
            return o;
        });

        // when
        Order result = createOrderService.createOrder(command);

        // then
        assertThat(result.getId()).isEqualTo(100L);
        verify(sendOrderNotificationPort, times(1)).sendOrderConfirmation(userEmail, result);
    }
}
