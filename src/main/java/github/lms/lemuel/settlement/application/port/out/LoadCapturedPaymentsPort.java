package github.lms.lemuel.settlement.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 생성을 위한 캡처된 결제 조회 Outbound Port
 * Payment 모듈과의 의존성을 인터페이스로 분리
 */
public interface LoadCapturedPaymentsPort {

    List<CapturedPaymentInfo> findCapturedPaymentsBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * 정산에 필요한 결제 정보만 담는 DTO
     */
    record CapturedPaymentInfo(
            Long paymentId,
            Long orderId,
            BigDecimal amount,
            BigDecimal refundedAmount,
            LocalDateTime capturedAt
    ) {
        public BigDecimal getSettlementAmount() {
            return amount.subtract(refundedAmount != null ? refundedAmount : BigDecimal.ZERO);
        }
    }
}
