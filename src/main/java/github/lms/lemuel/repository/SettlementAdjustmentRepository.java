package github.lms.lemuel.repository;

import github.lms.lemuel.domain.SettlementAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementAdjustmentRepository extends JpaRepository<SettlementAdjustment, Long> {

    /**
     * 환불 1건당 조정 1건 보장 체크
     */
    Optional<SettlementAdjustment> findByRefundId(Long refundId);

    /**
     * 특정 정산에 대한 모든 조정 조회
     */
    List<SettlementAdjustment> findBySettlementId(Long settlementId);

    /**
     * 특정 날짜의 PENDING 조정 조회 (배치 확정용)
     */
    @Query("SELECT sa FROM SettlementAdjustment sa WHERE sa.adjustmentDate = :date AND sa.status = 'PENDING'")
    List<SettlementAdjustment> findPendingByAdjustmentDate(LocalDate date);

    /**
     * 특정 정산의 PENDING 조정 조회
     */
    List<SettlementAdjustment> findBySettlementIdAndStatus(Long settlementId, SettlementAdjustment.AdjustmentStatus status);
}
