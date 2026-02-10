package github.lms.lemuel.monitoring;

import github.lms.lemuel.domain.Settlement;
import github.lms.lemuel.domain.SettlementAdjustment;
import github.lms.lemuel.repository.SettlementRepository;
import github.lms.lemuel.repository.SettlementAdjustmentRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 정산 배치 작업 상태 Health Indicator
 *
 * /actuator/health 엔드포인트에서 배치 작업 상태 확인
 */
@Component
public class SettlementBatchHealthIndicator implements HealthIndicator {

    private final SettlementRepository settlementRepository;
    private final SettlementAdjustmentRepository adjustmentRepository;

    public SettlementBatchHealthIndicator(SettlementRepository settlementRepository,
                                          SettlementAdjustmentRepository adjustmentRepository) {
        this.settlementRepository = settlementRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // 어제 날짜 기준 정산 상태 조회
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<Settlement> yesterdaySettlements = settlementRepository.findBySettlementDate(yesterday);

            long pendingCount = yesterdaySettlements.stream()
                    .filter(s -> s.getStatus() == Settlement.SettlementStatus.PENDING)
                    .count();

            long confirmedCount = yesterdaySettlements.stream()
                    .filter(s -> s.getStatus() == Settlement.SettlementStatus.CONFIRMED)
                    .count();

            // 어제 날짜 기준 정산 조정 상태 조회
            List<SettlementAdjustment> yesterdayAdjustments =
                    adjustmentRepository.findPendingByAdjustmentDate(yesterday);

            long pendingAdjustmentCount = yesterdayAdjustments.size();

            details.put("settlement_date", yesterday.toString());
            details.put("settlement_pending_count", pendingCount);
            details.put("settlement_confirmed_count", confirmedCount);
            details.put("adjustment_pending_count", pendingAdjustmentCount);

            // 상태 판단: PENDING이 너무 많으면 경고
            // 새벽 3시 이후에도 PENDING이 많으면 배치 실패 가능성
            if (pendingCount > 100) {
                return Health.down()
                        .withDetail("reason", "Too many pending settlements")
                        .withDetails(details)
                        .build();
            }

            if (pendingAdjustmentCount > 50) {
                return Health.status("WARNING")
                        .withDetail("reason", "Too many pending adjustments")
                        .withDetails(details)
                        .build();
            }

            return Health.up().withDetails(details).build();

        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}
