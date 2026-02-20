package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.PaymentStatus;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.application.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.application.port.out.PgClientPort;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizePaymentUseCase Unit Tests")
class AuthorizePaymentDomainUseCaseTest {

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private PgClientPort pgClientPort;

    @Mock
    private PublishEventPort publishEventPort;

    private AuthorizePaymentUseCase authorizePaymentUseCase;

    @BeforeEach
    void setUp() {
        authorizePaymentUseCase = new AuthorizePaymentUseCase(
            loadPaymentPort,
            savePaymentPort,
            pgClientPort,
            publishEventPort
        );
    }

    @Test
    @DisplayName("Success: Authorize payment in READY status")
    void shouldAuthorizePaymentSuccessfully() {
        // Given
        Long paymentId = 1L;
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");

        String pgTransactionId = "PG-12345";

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));
        given(pgClientPort.authorize(eq(paymentId), any(), eq("CARD"))).willReturn(pgTransactionId);
        given(savePaymentPort.save(any(PaymentDomain.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentDomain result = authorizePaymentUseCase.authorizePayment(paymentId);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(result.getPgTransactionId()).isEqualTo(pgTransactionId);

        verify(loadPaymentPort).loadById(paymentId);
        verify(pgClientPort).authorize(eq(paymentId), any(BigDecimal.class), eq("CARD"));
        verify(savePaymentPort).save(paymentDomain);
        verify(publishEventPort).publishPaymentAuthorized(paymentId);
    }

    @Test
    @DisplayName("Failure: Payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authorizePaymentUseCase.authorizePayment(paymentId))
            .isInstanceOf(PaymentNotFoundException.class)
            .hasMessageContaining("Payment not found: 999");

        verify(loadPaymentPort).loadById(paymentId);
        verifyNoInteractions(pgClientPort, savePaymentPort, publishEventPort);
    }

    @Test
    @DisplayName("Failure: Payment not in READY status")
    void shouldThrowExceptionWhenPaymentNotInReadyStatus() {
        // Given
        Long paymentId = 1L;
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");
        paymentDomain.authorize("PG-OLD"); // Already authorized

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));

        // When & Then
        assertThatThrownBy(() -> authorizePaymentUseCase.authorizePayment(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Payment must be in READY status to authorize");

        verify(loadPaymentPort).loadById(paymentId);
        verifyNoInteractions(savePaymentPort, publishEventPort);
    }

    @Test
    @DisplayName("Interaction: PG client called with correct parameters")
    void shouldCallPgClientWithCorrectParameters() {
        // Given
        Long paymentId = 1L;
        BigDecimal amount = new BigDecimal("10000.00");
        String paymentMethod = "CARD";

        PaymentDomain paymentDomain = new PaymentDomain(1L, amount, paymentMethod);

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));
        given(pgClientPort.authorize(paymentId, amount, paymentMethod)).willReturn("PG-12345");
        given(savePaymentPort.save(any(PaymentDomain.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        authorizePaymentUseCase.authorizePayment(paymentId);

        // Then
        ArgumentCaptor<Long> paymentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<String> methodCaptor = ArgumentCaptor.forClass(String.class);

        verify(pgClientPort).authorize(
            paymentIdCaptor.capture(),
            amountCaptor.capture(),
            methodCaptor.capture()
        );

        assertThat(paymentIdCaptor.getValue()).isEqualTo(paymentId);
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(amount);
        assertThat(methodCaptor.getValue()).isEqualTo(paymentMethod);
    }
}
