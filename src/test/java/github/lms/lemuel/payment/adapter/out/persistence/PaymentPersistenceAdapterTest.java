package github.lms.lemuel.payment.adapter.out.persistence;

import github.lms.lemuel.payment.domain.Payment;
import github.lms.lemuel.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({PaymentPersistenceAdapter.class, PaymentMapper.class})
@DisplayName("PaymentPersistenceAdapter DataJpa Tests")
class PaymentPersistenceAdapterTest {

    @Autowired
    private PaymentPersistenceAdapter paymentPersistenceAdapter;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Test
    @DisplayName("Success: Save new payment")
    void shouldSaveNewPaymentSuccessfully() {
        // Given
        Payment payment = new Payment(1L, new BigDecimal("10000.00"), "CARD");

        // When
        Payment savedPayment = paymentPersistenceAdapter.save(payment);

        // Then
        assertThat(savedPayment.getId()).isNotNull();
        assertThat(savedPayment.getOrderId()).isEqualTo(1L);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(savedPayment.getPaymentMethod()).isEqualTo("CARD");
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(savedPayment.getCreatedAt()).isNotNull();
        assertThat(savedPayment.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Success: Load payment by ID")
    void shouldLoadPaymentByIdSuccessfully() {
        // Given
        Payment payment = new Payment(1L, new BigDecimal("10000.00"), "CARD");
        Payment savedPayment = paymentPersistenceAdapter.save(payment);

        // When
        Optional<Payment> loadedPayment = paymentPersistenceAdapter.loadById(savedPayment.getId());

        // Then
        assertThat(loadedPayment).isPresent();
        assertThat(loadedPayment.get().getId()).isEqualTo(savedPayment.getId());
        assertThat(loadedPayment.get().getOrderId()).isEqualTo(1L);
        assertThat(loadedPayment.get().getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Success: Load payment by order ID")
    void shouldLoadPaymentByOrderIdSuccessfully() {
        // Given
        Long orderId = 100L;
        Payment payment = new Payment(orderId, new BigDecimal("10000.00"), "CARD");
        paymentPersistenceAdapter.save(payment);

        // When
        Optional<Payment> loadedPayment = paymentPersistenceAdapter.loadByOrderId(orderId);

        // Then
        assertThat(loadedPayment).isPresent();
        assertThat(loadedPayment.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Success: Update existing payment")
    void shouldUpdateExistingPaymentSuccessfully() {
        // Given
        Payment payment = new Payment(1L, new BigDecimal("10000.00"), "CARD");
        Payment savedPayment = paymentPersistenceAdapter.save(payment);

        // When: Authorize payment
        savedPayment.authorize("PG-12345");
        Payment updatedPayment = paymentPersistenceAdapter.save(savedPayment);

        // Then
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(updatedPayment.getPgTransactionId()).isEqualTo("PG-12345");
    }

    @Test
    @DisplayName("Failure: Load non-existent payment by ID")
    void shouldReturnEmptyWhenPaymentNotFoundById() {
        // When
        Optional<Payment> loadedPayment = paymentPersistenceAdapter.loadById(999L);

        // Then
        assertThat(loadedPayment).isEmpty();
    }

    @Test
    @DisplayName("Failure: Load non-existent payment by order ID")
    void shouldReturnEmptyWhenPaymentNotFoundByOrderId() {
        // When
        Optional<Payment> loadedPayment = paymentPersistenceAdapter.loadByOrderId(999L);

        // Then
        assertThat(loadedPayment).isEmpty();
    }

    @Test
    @DisplayName("Mapping: Verify full lifecycle state transitions are persisted")
    void shouldPersistFullPaymentLifecycle() {
        // Given: Create and save payment
        Payment payment = new Payment(1L, new BigDecimal("10000.00"), "CARD");
        Payment savedPayment = paymentPersistenceAdapter.save(payment);
        Long paymentId = savedPayment.getId();

        // When & Then: Authorize
        savedPayment.authorize("PG-12345");
        Payment authorizedPayment = paymentPersistenceAdapter.save(savedPayment);
        assertThat(authorizedPayment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

        // When & Then: Capture
        Payment reloadedPayment = paymentPersistenceAdapter.loadById(paymentId).get();
        reloadedPayment.capture();
        Payment capturedPayment = paymentPersistenceAdapter.save(reloadedPayment);
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(capturedPayment.getCapturedAt()).isNotNull();

        // When & Then: Refund
        Payment reloadedPayment2 = paymentPersistenceAdapter.loadById(paymentId).get();
        reloadedPayment2.refund();
        Payment refundedPayment = paymentPersistenceAdapter.save(reloadedPayment2);
        assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
