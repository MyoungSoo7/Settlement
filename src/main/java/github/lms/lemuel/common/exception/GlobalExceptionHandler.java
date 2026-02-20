package github.lms.lemuel.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RefundExceedsPaymentException.class)
    public ResponseEntity<Map<String, Object>> handleRefundExceedsPayment(RefundExceedsPaymentException ex) {
        logger.warn("환불 금액 초과: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "REFUND_EXCEEDS_PAYMENT", ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPaymentState(InvalidPaymentStateException ex) {
        logger.warn("잘못된 결제 상태: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATE", ex.getMessage());
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<Map<String, Object>> handleMissingIdempotencyKey(MissingIdempotencyKeyException ex) {
        logger.warn("Idempotency-Key 누락: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", ex.getMessage());
    }

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<Map<String, Object>> handleRefundException(RefundException ex) {
        logger.error("환불 처리 오류: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "REFUND_ERROR", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String errorCode, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
