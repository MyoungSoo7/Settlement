package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.Payment;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.port.in.AuthorizePaymentPort;
import github.lms.lemuel.payment.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.port.out.PgClientPort;
import github.lms.lemuel.payment.port.out.PublishEventPort;
import github.lms.lemuel.payment.port.out.SavePaymentPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthorizePaymentUseCase implements AuthorizePaymentPort {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PgClientPort pgClientPort;
    private final PublishEventPort publishEventPort;

    public AuthorizePaymentUseCase(LoadPaymentPort loadPaymentPort,
                                   SavePaymentPort savePaymentPort,
                                   PgClientPort pgClientPort,
                                   PublishEventPort publishEventPort) {
        this.loadPaymentPort = loadPaymentPort;
        this.savePaymentPort = savePaymentPort;
        this.pgClientPort = pgClientPort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public Payment authorizePayment(Long paymentId) {
        Payment payment = loadPaymentPort.loadById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Call external PG to authorize
        String pgTransactionId = pgClientPort.authorize(
            payment.getId(),
            payment.getAmount(),
            payment.getPaymentMethod()
        );

        // Domain logic
        payment.authorize(pgTransactionId);

        // Save and publish event
        Payment savedPayment = savePaymentPort.save(payment);
        publishEventPort.publishPaymentAuthorized(savedPayment.getId());

        return savedPayment;
    }
}
