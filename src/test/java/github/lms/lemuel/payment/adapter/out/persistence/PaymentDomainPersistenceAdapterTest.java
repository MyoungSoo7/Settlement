package github.lms.lemuel.payment.adapter.out.persistence;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({PaymentPersistenceAdapter.class, PaymentMapper.class})
@DisplayName("PaymentPersistenceAdapter DataJpa Tests")
class PaymentDomainPersistenceAdapterTest {

    @Autowired
    private PaymentPersistenceAdapter paymentPersistenceAdapter;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Test
    @DisplayName("Success: Save new payment")
    void shouldSaveNewPaymentSuccessfully() {
        // Given
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");

        // When
        PaymentDomain savedPaymentDomain = paymentPersistenceAdapter.save(paymentDomain);

        // Then
        assertThat(savedPaymentDomain.getId()).isNotNull();
        assertThat(savedPaymentDomain.getOrderId()).isEqualTo(1L);
        assertThat(savedPaymentDomain.getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(savedPaymentDomain.getPaymentMethod()).isEqualTo("CARD");
        assertThat(savedPaymentDomain.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(savedPaymentDomain.getCreatedAt()).isNotNull();
        assertThat(savedPaymentDomain.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Success: Load payment by ID")
    void shouldLoadPaymentByIdSuccessfully() {
        // Given
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");
        PaymentDomain savedPaymentDomain = paymentPersistenceAdapter.save(paymentDomain);

        // When
        Optional<PaymentDomain> loadedPayment = paymentPersistenceAdapter.loadById(savedPaymentDomain.getId());

        // Then
        assertThat(loadedPayment).isPresent();
        assertThat(loadedPayment.get().getId()).isEqualTo(savedPaymentDomain.getId());
        assertThat(loadedPayment.get().getOrderId()).isEqualTo(1L);
        assertThat(loadedPayment.get().getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Success: Load payment by order ID")
    void shouldLoadPaymentByOrderIdSuccessfully() {
        // Given
        Long orderId = 100L;
        PaymentDomain paymentDomain = new PaymentDomain(orderId, new BigDecimal("10000.00"), "CARD");
        paymentPersistenceAdapter.save(paymentDomain);

        // When
        Optional<PaymentDomain> loadedPayment = paymentPersistenceAdapter.loadByOrderId(orderId);

        // Then
        assertThat(loadedPayment).isPresent();
        assertThat(loadedPayment.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Success: Update existing payment")
    void shouldUpdateExistingPaymentSuccessfully() {
        // Given
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");
        PaymentDomain savedPaymentDomain = paymentPersistenceAdapter.save(paymentDomain);

        // When: Authorize payment
        savedPaymentDomain.authorize("PG-12345");
        PaymentDomain updatedPaymentDomain = paymentPersistenceAdapter.save(savedPaymentDomain);

        // Then
        assertThat(updatedPaymentDomain.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(updatedPaymentDomain.getPgTransactionId()).isEqualTo("PG-12345");
    }

    @Test
    @DisplayName("Failure: Load non-existent payment by ID")
    void shouldReturnEmptyWhenPaymentNotFoundById() {
        // When
        Optional<PaymentDomain> loadedPayment = paymentPersistenceAdapter.loadById(999L);

        // Then
        assertThat(loadedPayment).isEmpty();
    }

    @Test
    @DisplayName("Failure: Load non-existent payment by order ID")
    void shouldReturnEmptyWhenPaymentNotFoundByOrderId() {
        // When
        Optional<PaymentDomain> loadedPayment = paymentPersistenceAdapter.loadByOrderId(999L);

        // Then
        assertThat(loadedPayment).isEmpty();
    }

    @Test
    @DisplayName("Mapping: Verify full lifecycle state transitions are persisted")
    void shouldPersistFullPaymentLifecycle() {
        // Given: Create and save payment
        PaymentDomain paymentDomain = new PaymentDomain(1L, new BigDecimal("10000.00"), "CARD");
        PaymentDomain savedPaymentDomain = paymentPersistenceAdapter.save(paymentDomain);
        Long paymentId = savedPaymentDomain.getId();

        // When & Then: Authorize
        savedPaymentDomain.authorize("PG-12345");
        PaymentDomain authorizedPaymentDomain = paymentPersistenceAdapter.save(savedPaymentDomain);
        assertThat(authorizedPaymentDomain.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

        // When & Then: Capture
        PaymentDomain reloadedPaymentDomain = paymentPersistenceAdapter.loadById(paymentId).get();
        reloadedPaymentDomain.capture();
        PaymentDomain capturedPaymentDomain = paymentPersistenceAdapter.save(reloadedPaymentDomain);
        assertThat(capturedPaymentDomain.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(capturedPaymentDomain.getCapturedAt()).isNotNull();

        // When & Then: Refund
        PaymentDomain reloadedPaymentDomain2 = paymentPersistenceAdapter.loadById(paymentId).get();
        reloadedPaymentDomain2.refund();
        PaymentDomain refundedPaymentDomain = paymentPersistenceAdapter.save(reloadedPaymentDomain2);
        assertThat(refundedPaymentDomain.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
