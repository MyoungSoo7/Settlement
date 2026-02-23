package github.lms.lemuel.settlement.application.port.in;

import github.lms.lemuel.settlement.domain.Settlement;

import java.math.BigDecimal;

/**
 * Use Case: 환불 발생 시 정산 조정
 */
public interface AdjustSettlementForRefundUseCase {

    /**
     * 환불 금액을 정산에 반영하여 순 정산 금액 재계산
     *
     * @param paymentId 결제 ID
     * @param refundAmount 환불 금액
     * @return 조정된 정산
     */
    Settlement adjustSettlementForRefund(Long paymentId, BigDecimal refundAmount);
}
