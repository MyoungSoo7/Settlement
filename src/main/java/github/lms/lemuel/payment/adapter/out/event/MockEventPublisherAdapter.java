package github.lms.lemuel.payment.adapter.out.event;

import github.lms.lemuel.payment.application.port.out.PublishEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock event publisher adapter (replace with real implementation like Kafka, RabbitMQ)
 */
@Component
public class MockEventPublisherAdapter implements PublishEventPort {

    private static final Logger log = LoggerFactory.getLogger(MockEventPublisherAdapter.class);

    @Override
    public void publishPaymentCreated(Long paymentId, Long orderId) {
        log.info("Event published: PaymentCreated [paymentId={}, orderId={}]", paymentId, orderId);
    }

    @Override
    public void publishPaymentAuthorized(Long paymentId) {
        log.info("Event published: PaymentAuthorized [paymentId={}]", paymentId);
    }

    @Override
    public void publishPaymentCaptured(Long paymentId, Long orderId) {
        log.info("Event published: PaymentCaptured [paymentId={}, orderId={}]", paymentId, orderId);
    }

    @Override
    public void publishPaymentRefunded(Long paymentId, Long orderId) {
        log.info("Event published: PaymentRefunded [paymentId={}, orderId={}]", paymentId, orderId);
    }
}
