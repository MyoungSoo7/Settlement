package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.PaymentStatus;
import github.lms.lemuel.payment.domain.exception.InvalidOrderStateException;
import github.lms.lemuel.payment.domain.exception.OrderNotFoundException;
import github.lms.lemuel.payment.application.port.in.CreatePaymentCommand;
import github.lms.lemuel.payment.application.port.out.LoadOrderPort;
import github.lms.lemuel.payment.application.port.out.LoadOrderPort.OrderInfo;
import github.lms.lemuel.payment.application.port.out.PublishEventPort;
import github.lms.lemuel.payment.application.port.out.SavePaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePaymentUseCase Unit Tests")
class CreatePaymentDomainUseCaseTest {

    @Mock
    private LoadOrderPort loadOrderPort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private PublishEventPort publishEventPort;

    private CreatePaymentUseCase createPaymentUseCase;

    @BeforeEach
    void setUp() {
        createPaymentUseCase = new CreatePaymentUseCase(
            loadOrderPort,
            savePaymentPort,
            publishEventPort
        );
    }

    @Test
    @DisplayName("Success: Create payment for valid order")
    void shouldCreatePaymentSuccessfully() {
        // Given
        Long orderId = 1L;
        BigDecimal amount = new BigDecimal("10000.00");
        String paymentMethod = "CARD";

        OrderInfo orderInfo = new OrderInfo(orderId, amount, "CREATED");
        given(loadOrderPort.loadOrder(orderId)).willReturn(orderInfo);

        PaymentDomain savedPaymentDomain = new PaymentDomain(orderId, amount, paymentMethod);
        given(savePaymentPort.save(any(PaymentDomain.class))).willReturn(savedPaymentDomain);

        CreatePaymentCommand command = new CreatePaymentCommand(orderId, paymentMethod);

        // When
        PaymentDomain result = createPaymentUseCase.createPayment(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.READY);

        verify(loadOrderPort).loadOrder(orderId);
        verify(savePaymentPort).save(any(PaymentDomain.class));
        verify(publishEventPort).publishPaymentCreated(any(), eq(orderId));
    }

    @Test
    @DisplayName("Failure: Order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        Long orderId = 999L;
        given(loadOrderPort.loadOrder(orderId)).willReturn(null);

        CreatePaymentCommand command = new CreatePaymentCommand(orderId, "CARD");

        // When & Then
        assertThatThrownBy(() -> createPaymentUseCase.createPayment(command))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessageContaining("Order not found: 999");

        verify(loadOrderPort).loadOrder(orderId);
        verifyNoInteractions(savePaymentPort, publishEventPort);
    }

    @Test
    @DisplayName("Failure: Order not in CREATED status")
    void shouldThrowExceptionWhenOrderNotInCreatedStatus() {
        // Given
        Long orderId = 1L;
        OrderInfo orderInfo = new OrderInfo(orderId, new BigDecimal("10000.00"), "PAID");
        given(loadOrderPort.loadOrder(orderId)).willReturn(orderInfo);

        CreatePaymentCommand command = new CreatePaymentCommand(orderId, "CARD");

        // When & Then
        assertThatThrownBy(() -> createPaymentUseCase.createPayment(command))
            .isInstanceOf(InvalidOrderStateException.class)
            .hasMessageContaining("Order must be in CREATED status");

        verify(loadOrderPort).loadOrder(orderId);
        verifyNoInteractions(savePaymentPort, publishEventPort);
    }

    @Test
    @DisplayName("Interaction: Verify event is published with correct parameters")
    void shouldPublishEventWithCorrectParameters() {
        // Given
        Long orderId = 1L;
        Long paymentId = 100L;
        OrderInfo orderInfo = new OrderInfo(orderId, new BigDecimal("10000.00"), "CREATED");
        given(loadOrderPort.loadOrder(orderId)).willReturn(orderInfo);

        PaymentDomain savedPaymentDomain = new PaymentDomain(
            paymentId, orderId, new BigDecimal("10000.00"), BigDecimal.ZERO,
            PaymentStatus.READY, "CARD", null, null, null, null
        );
        given(savePaymentPort.save(any(PaymentDomain.class))).willReturn(savedPaymentDomain);

        CreatePaymentCommand command = new CreatePaymentCommand(orderId, "CARD");

        // When
        createPaymentUseCase.createPayment(command);

        // Then
        ArgumentCaptor<Long> paymentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> orderIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(publishEventPort).publishPaymentCreated(paymentIdCaptor.capture(), orderIdCaptor.capture());

        assertThat(paymentIdCaptor.getValue()).isEqualTo(paymentId);
        assertThat(orderIdCaptor.getValue()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Validation: Command with null orderId should throw exception")
    void shouldRejectNullOrderId() {
        // When & Then
        assertThatThrownBy(() -> new CreatePaymentCommand(null, "CARD"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("orderId must not be null");
    }

    @Test
    @DisplayName("Validation: Command with blank paymentMethod should throw exception")
    void shouldRejectBlankPaymentMethod() {
        // When & Then
        assertThatThrownBy(() -> new CreatePaymentCommand(1L, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("paymentMethod must not be blank");
    }
}
