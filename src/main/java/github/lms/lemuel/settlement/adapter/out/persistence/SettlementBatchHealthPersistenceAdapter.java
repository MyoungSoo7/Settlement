package github.lms.lemuel.settlement.adapter.out.persistence;

import github.lms.lemuel.settlement.application.dto.SettlementBatchHealthSnapshot;
import github.lms.lemuel.settlement.application.port.out.LoadSettlementBatchHealthPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Settlement Batch Health Persistence Adapter (Outbound)
 * 배치 헬스 체크를 위한 데이터 조회 어댑터
 */
@Component
public class SettlementBatchHealthPersistenceAdapter implements LoadSettlementBatchHealthPort {

    private final SpringDataSettlementJpaRepository settlementRepository;
    // TODO: SettlementAdjustment가 클린 아키텍처로 이동되면 해당 Repository 주입
    // private final SpringDataSettlementAdjustmentJpaRepository adjustmentRepository;

    public SettlementBatchHealthPersistenceAdapter(SpringDataSettlementJpaRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
        // this.adjustmentRepository = adjustmentRepository;
    }

    @Override
    public SettlementBatchHealthSnapshot loadHealthSnapshot(LocalDate date) {
        // 정산 데이터 조회
        List<SettlementJpaEntity> settlements = settlementRepository.findBySettlementDate(date);

        long pendingCount = settlements.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .count();

        long confirmedCount = settlements.stream()
                .filter(s -> "CONFIRMED".equals(s.getStatus()))
                .count();

        // TODO: SettlementAdjustment 조회
        // SettlementAdjustment가 클린 아키텍처로 이동되면 아래 주석 해제
        // List<SettlementAdjustmentJpaEntity> adjustments =
        //     adjustmentRepository.findPendingByAdjustmentDate(date);
        // long pendingAdjustmentCount = adjustments.size();

        // 임시: adjustment 카운트는 0으로 처리 (SettlementAdjustment 이동 전)
        long pendingAdjustmentCount = 0;

        return new SettlementBatchHealthSnapshot(
                date,
                pendingCount,
                confirmedCount,
                pendingAdjustmentCount
        );
    }
}
