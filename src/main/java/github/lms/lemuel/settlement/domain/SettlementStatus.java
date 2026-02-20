package github.lms.lemuel.settlement.domain;

/**
 * 정산 상태 Enum
 */
public enum SettlementStatus {
    PENDING,            // 정산 대기
    WAITING_APPROVAL,   // 승인 대기
    CONFIRMED,          // 정산 확정
    CANCELED;           // 정산 취소

    public static SettlementStatus fromString(String status) {
        try {
            return SettlementStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return PENDING; // 기본값
        }
    }
}
