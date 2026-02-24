package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.PaymentStatus;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.application.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.application.port.out.PgClientPort;
import github.lms.lemuel.payment.application.port.out.PublishEventPort;
import github.lms.lemuel.payment.application.port.out.SavePaymentPort;
import github.lms.lemuel.payment.application.port.out.UpdateOrderStatusPort;
import github.lms.lemuel.settlement.application.port.in.CreateSettlementFromPaymentUseCase;
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
@DisplayName("CapturePaymentUseCase Unit Tests")
class CapturePaymentDomainUseCaseTest {

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

    @Mock
    private CreateSettlementFromPaymentUseCase createSettlementFromPaymentUseCase;

    private CapturePaymentUseCase capturePaymentUseCase;

    @BeforeEach
    void setUp() {
        capturePaymentUseCase = new CapturePaymentUseCase(
            loadPaymentPort,
            savePaymentPort,
            pgClientPort,
            updateOrderStatusPort,
            publishEventPort,
            createSettlementFromPaymentUseCase
        );
    }

    @Test
    @DisplayName("Success: Capture authorized payment")
    void shouldCapturePaymentSuccessfully() {
        // Given
        Long paymentId = 1L;
        Long orderId = 10L;
        BigDecimal amount = new BigDecimal("10000.00");
        String pgTransactionId = "PG-12345";

        PaymentDomain paymentDomain = new PaymentDomain(orderId, amount, "CARD");
        paymentDomain.authorize(pgTransactionId);

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));
        given(savePaymentPort.save(any(PaymentDomain.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentDomain result = capturePaymentUseCase.capturePayment(paymentId);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(result.getCapturedAt()).isNotNull();

        verify(loadPaymentPort).loadById(paymentId);
        verify(pgClientPort).capture(pgTransactionId, amount);
        verify(savePaymentPort).save(paymentDomain);
        verify(updateOrderStatusPort).updateOrderStatus(orderId, "PAID");
        verify(publishEventPort).publishPaymentCaptured(any(), eq(orderId));
    }

    @Test
    @DisplayName("Failure: Payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> capturePaymentUseCase.capturePayment(paymentId))
            .isInstanceOf(PaymentNotFoundException.class)
            .hasMessageContaining("Payment not found: 999");

        verifyNoInteractions(pgClientPort, savePaymentPort, updateOrderStatusPort, publishEventPort);
    }

    @Test
    @DisplayName("Failure: Payment not in AUTHORIZED status")
    void shouldThrowExceptionWhenPaymentNotInAuthorizedStatus() {
        // Given
        Long paymentId = 1L;
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");
        // Payment is in READY status, not AUTHORIZED

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));

        // When & Then
        assertThatThrownBy(() -> capturePaymentUseCase.capturePayment(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Payment must be in AUTHORIZED status to capture");

        verify(loadPaymentPort).loadById(paymentId);
        verifyNoInteractions(savePaymentPort, updateOrderStatusPort, publishEventPort);
    }

    @Test
    @DisplayName("Interaction: Verify execution order - capture before update order")
    void shouldExecuteInCorrectOrder() {
        // Given
        Long paymentId = 1L;
        Long orderId = 10L;
        PaymentDomain paymentDomain = new PaymentDomain(orderId, new BigDecimal("10000.00"), "CARD");
        paymentDomain.authorize("PG-12345");

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));
        given(savePaymentPort.save(any(PaymentDomain.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        capturePaymentUseCase.capturePayment(paymentId);

        // Then: Verify order of operations
        InOrder inOrder = inOrder(pgClientPort, savePaymentPort, updateOrderStatusPort, publishEventPort);
        inOrder.verify(pgClientPort).capture(any(), any());
        inOrder.verify(savePaymentPort).save(any());
        inOrder.verify(updateOrderStatusPort).updateOrderStatus(orderId, "PAID");
        inOrder.verify(publishEventPort).publishPaymentCaptured(any(), eq(orderId));
    }
}
