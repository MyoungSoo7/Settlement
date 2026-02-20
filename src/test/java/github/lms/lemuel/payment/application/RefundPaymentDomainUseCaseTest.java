package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.PaymentStatus;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.application.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.application.port.out.PgClientPort;
import github.lms.lemuel.payment.application.port.out.PublishEventPort;
import github.lms.lemuel.payment.application.port.out.SavePaymentPort;
import github.lms.lemuel.payment.application.port.out.UpdateOrderStatusPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundPaymentUseCase Unit Tests")
class RefundPaymentDomainUseCaseTest {

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private PgClientPort pgClientPort;

    @Mock
    private UpdateOrderStatusPort updateOrderStatusPort;

    @Mock
    private PublishEventPort publishEventPort;

    private RefundPaymentUseCase refundPaymentUseCase;

    @BeforeEach
    void setUp() {
        refundPaymentUseCase = new RefundPaymentUseCase(
            loadPaymentPort,
            savePaymentPort,
            pgClientPort,
            updateOrderStatusPort,
            publishEventPort
        );
    }

    @Test
    @DisplayName("Success: Refund captured payment")
    void shouldRefundPaymentSuccessfully() {
        // Given
        Long paymentId = 1L;
        Long orderId = 10L;
        BigDecimal amount = new BigDecimal("10000.00");
        String pgTransactionId = "PG-12345";

        PaymentDomain paymentDomain = new PaymentDomain(orderId, amount, "CARD");
        paymentDomain.authorize(pgTransactionId);
        paymentDomain.capture();

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));
        given(savePaymentPort.save(any(PaymentDomain.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentDomain result = refundPaymentUseCase.refundPayment(paymentId);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        verify(loadPaymentPort).loadById(paymentId);
        verify(pgClientPort).refund(pgTransactionId, amount);
        verify(savePaymentPort).save(paymentDomain);
        verify(updateOrderStatusPort).updateOrderStatus(orderId, "REFUNDED");
        verify(publishEventPort).publishPaymentRefunded(any(), eq(orderId));
    }

    @Test
    @DisplayName("Failure: Payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refundPaymentUseCase.refundPayment(paymentId))
            .isInstanceOf(PaymentNotFoundException.class)
            .hasMessageContaining("Payment not found: 999");

        verifyNoInteractions(pgClientPort, savePaymentPort, updateOrderStatusPort, publishEventPort);
    }

    @Test
    @DisplayName("Failure: Payment not in CAPTURED status")
    void shouldThrowExceptionWhenPaymentNotInCapturedStatus() {
        // Given
        Long paymentId = 1L;
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");
        paymentDomain.authorize("PG-12345");
        // Payment is AUTHORIZED, not CAPTURED

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));

        // When & Then
        assertThatThrownBy(() -> refundPaymentUseCase.refundPayment(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Payment must be in CAPTURED status to refund");

        verify(loadPaymentPort).loadById(paymentId);
        verifyNoInteractions(savePaymentPort, updateOrderStatusPort, publishEventPort);
    }

    @Test
    @DisplayName("Interaction: Verify execution order - refund before update order")
    void shouldExecuteInCorrectOrder() {
        // Given
        Long paymentId = 1L;
        Long orderId = 10L;
        PaymentDomain paymentDomain = new PaymentDomain(orderId, new BigDecimal("10000.00"), "CARD");
        paymentDomain.authorize("PG-12345");
        paymentDomain.capture();

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));
        given(savePaymentPort.save(any(PaymentDomain.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        refundPaymentUseCase.refundPayment(paymentId);

        // Then: Verify order of operations
        InOrder inOrder = inOrder(pgClientPort, savePaymentPort, updateOrderStatusPort, publishEventPort);
        inOrder.verify(pgClientPort).refund(any(), any());
        inOrder.verify(savePaymentPort).save(any());
        inOrder.verify(updateOrderStatusPort).updateOrderStatus(orderId, "REFUNDED");
        inOrder.verify(publishEventPort).publishPaymentRefunded(any(), eq(orderId));
    }
}
