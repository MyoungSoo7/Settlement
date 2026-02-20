package github.lms.lemuel.payment.application.port.out;

/**
 * Port for publishing domain events
 */
public interface PublishEventPort {
    void publishPaymentCreated(Long paymentId, Long orderId);
    void publishPaymentAuthorized(Long paymentId);
    void publishPaymentCaptured(Long paymentId, Long orderId);
    void publishPaymentRefunded(Long paymentId, Long orderId);
}
