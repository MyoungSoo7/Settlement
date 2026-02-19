package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.Payment;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.port.in.RefundPaymentPort;
import github.lms.lemuel.payment.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.port.out.PgClientPort;
import github.lms.lemuel.payment.port.out.PublishEventPort;
import github.lms.lemuel.payment.port.out.SavePaymentPort;
import github.lms.lemuel.payment.port.out.UpdateOrderStatusPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefundPaymentUseCase implements RefundPaymentPort {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PgClientPort pgClientPort;
    private final UpdateOrderStatusPort updateOrderStatusPort;
    private final PublishEventPort publishEventPort;

    public RefundPaymentUseCase(LoadPaymentPort loadPaymentPort,
                                SavePaymentPort savePaymentPort,
                                PgClientPort pgClientPort,
                                UpdateOrderStatusPort updateOrderStatusPort,
                                PublishEventPort publishEventPort) {
        this.loadPaymentPort = loadPaymentPort;
        this.savePaymentPort = savePaymentPort;
        this.pgClientPort = pgClientPort;
        this.updateOrderStatusPort = updateOrderStatusPort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public Payment refundPayment(Long paymentId) {
        Payment payment = loadPaymentPort.loadById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Call external PG to refund
        pgClientPort.refund(payment.getPgTransactionId(), payment.getAmount());

        // Domain logic
        payment.refund();

        // Save payment
        Payment savedPayment = savePaymentPort.save(payment);

        // Update order status to REFUNDED
        updateOrderStatusPort.updateOrderStatus(savedPayment.getOrderId(), "REFUNDED");

        // Publish event
        publishEventPort.publishPaymentRefunded(savedPayment.getId(), savedPayment.getOrderId());

        return savedPayment;
    }
}
