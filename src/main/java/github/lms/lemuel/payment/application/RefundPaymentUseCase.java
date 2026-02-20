package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.application.port.in.RefundPaymentPort;
import github.lms.lemuel.payment.application.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.application.port.out.PgClientPort;
import github.lms.lemuel.payment.application.port.out.PublishEventPort;
import github.lms.lemuel.payment.application.port.out.SavePaymentPort;
import github.lms.lemuel.payment.application.port.out.UpdateOrderStatusPort;
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
    public PaymentDomain refundPayment(Long paymentId) {
        PaymentDomain paymentDomain = loadPaymentPort.loadById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Call external PG to refund
        pgClientPort.refund(paymentDomain.getPgTransactionId(), paymentDomain.getAmount());

        // Domain logic
        paymentDomain.refund();

        // Save payment
        PaymentDomain savedPaymentDomain = savePaymentPort.save(paymentDomain);

        // Update order status to REFUNDED
        updateOrderStatusPort.updateOrderStatus(savedPaymentDomain.getOrderId(), "REFUNDED");

        // Publish event
        publishEventPort.publishPaymentRefunded(savedPaymentDomain.getId(), savedPaymentDomain.getOrderId());

        return savedPaymentDomain;
    }
}
