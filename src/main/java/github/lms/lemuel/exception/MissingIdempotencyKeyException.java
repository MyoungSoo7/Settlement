package github.lms.lemuel.exception;

public class MissingIdempotencyKeyException extends RefundException {
    public MissingIdempotencyKeyException(String message) {
        super(message);
    }
}
