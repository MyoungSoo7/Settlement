package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.PaymentStatus;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.application.port.out.LoadPaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPaymentUseCase Unit Tests")
class GetPaymentDomainUseCaseTest {

    @Mock
    private LoadPaymentPort loadPaymentPort;

    private GetPaymentUseCase getPaymentUseCase;

    @BeforeEach
    void setUp() {
        getPaymentUseCase = new GetPaymentUseCase(loadPaymentPort);
    }

    @Test
    @DisplayName("Success: Get payment by ID")
    void shouldGetPaymentSuccessfully() {
        // Given
        Long paymentId = 1L;
        PaymentDomain paymentDomain = new PaymentDomain(
            paymentId, 10L, new BigDecimal("10000.00"), BigDecimal.ZERO,
            PaymentStatus.READY, "CARD", null, null, null, null
        );

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));

        // When
        PaymentDomain result = getPaymentUseCase.getPayment(paymentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.READY);

        verify(loadPaymentPort).loadById(paymentId);
    }

    @Test
    @DisplayName("Failure: Payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> getPaymentUseCase.getPayment(paymentId))
            .isInstanceOf(PaymentNotFoundException.class)
            .hasMessageContaining("Payment not found: 999");

        verify(loadPaymentPort).loadById(paymentId);
    }

    @Test
    @DisplayName("Interaction: LoadPaymentPort should be called exactly once")
    void shouldCallLoadPaymentPortOnce() {
        // Given
        Long paymentId = 1L;
        PaymentDomain paymentDomain = new PaymentDomain(10L, new BigDecimal("10000.00"), "CARD");

        given(loadPaymentPort.loadById(paymentId)).willReturn(Optional.of(paymentDomain));

        // When
        getPaymentUseCase.getPayment(paymentId);

        // Then
        verify(loadPaymentPort).loadById(paymentId);
    }
}
