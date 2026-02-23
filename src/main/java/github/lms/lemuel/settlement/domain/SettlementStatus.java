package github.lms.lemuel.settlement.domain;

/**
 * 정산 상태 Enum
 *
 * 상태 전이:
 * REQUESTED → PROCESSING → DONE
 *           ↘            ↘ FAILED
 */
public enum SettlementStatus {
    REQUESTED,          // 정산 요청됨 (초기 상태)
    PROCESSING,         // 정산 처리 중
    DONE,               // 정산 완료
    FAILED,             // 정산 실패
    PENDING,            // 정산 대기 (레거시 호환)
    WAITING_APPROVAL,   // 승인 대기
    APPROVED,           // 승인됨
    REJECTED,           // 거부됨
    CONFIRMED,          // 정산 확정
    CANCELED,           // 정산 취소
    CALCULATED;         // 계산됨

    public static SettlementStatus fromString(String status) {
        try {
            return SettlementStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return REQUESTED; // 기본값
        }
    }

    public boolean canTransitionTo(SettlementStatus targetStatus) {
        switch (this) {
            case REQUESTED:
                return targetStatus == PROCESSING || targetStatus == CANCELED;
            case PROCESSING:
                return targetStatus == DONE || targetStatus == FAILED;
            case FAILED:
                return targetStatus == REQUESTED; // 재시도 가능
            case DONE:
            case CANCELED:
                return false; // 종료 상태
            default:
                return false;
        }
    }
}
