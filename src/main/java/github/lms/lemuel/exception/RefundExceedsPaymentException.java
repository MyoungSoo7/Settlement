package github.lms.lemuel.exception;

public class RefundExceedsPaymentException extends RefundException {
    public RefundExceedsPaymentException(String message) {
        super(message);
    }
}
