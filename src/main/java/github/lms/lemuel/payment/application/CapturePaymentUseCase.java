package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.application.port.in.CapturePaymentPort;
import github.lms.lemuel.payment.application.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.application.port.out.PgClientPort;
import github.lms.lemuel.payment.application.port.out.PublishEventPort;
import github.lms.lemuel.payment.application.port.out.SavePaymentPort;
import github.lms.lemuel.payment.application.port.out.UpdateOrderStatusPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CapturePaymentUseCase implements CapturePaymentPort {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PgClientPort pgClientPort;
    private final UpdateOrderStatusPort updateOrderStatusPort;
    private final PublishEventPort publishEventPort;

    public CapturePaymentUseCase(LoadPaymentPort loadPaymentPort,
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
    public PaymentDomain capturePayment(Long paymentId) {
        PaymentDomain paymentDomain = loadPaymentPort.loadById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Call external PG to capture
        pgClientPort.capture(paymentDomain.getPgTransactionId(), paymentDomain.getAmount());

        // Domain logic
        paymentDomain.capture();

        // Save payment
        PaymentDomain savedPaymentDomain = savePaymentPort.save(paymentDomain);

        // Update order status to PAID
        updateOrderStatusPort.updateOrderStatus(savedPaymentDomain.getOrderId(), "PAID");

        // Publish event
        publishEventPort.publishPaymentCaptured(savedPaymentDomain.getId(), savedPaymentDomain.getOrderId());

        return savedPaymentDomain;
    }
}
