package github.lms.lemuel.exception;

public class InvalidPaymentStateException extends RefundException {
    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
