package github.lms.lemuel.order.domain.exception;

public class UserNotExistsException extends RuntimeException {
    public UserNotExistsException(Long userId) {
        super("User does not exist with id: " + userId);
    }
}
