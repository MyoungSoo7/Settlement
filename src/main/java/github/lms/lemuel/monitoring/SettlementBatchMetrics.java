package github.lms.lemuel.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 정산 배치 작업 메트릭 수집
 *
 * Prometheus, Grafana 등 외부 모니터링 시스템과 연동
 */
@Component
public class SettlementBatchMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter settlementCreatedCounter;
    private final Counter settlementConfirmedCounter;
    private final Counter adjustmentConfirmedCounter;

    private final Timer settlementCreationTimer;
    private final Timer settlementConfirmationTimer;
    private final Timer adjustmentConfirmationTimer;

    public SettlementBatchMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 정산 생성 건수
        this.settlementCreatedCounter = Counter.builder("settlement.batch.created")
            .description("Number of settlements created by batch")
            .tag("batch", "settlement_creation")
            .register(meterRegistry);

        // 정산 확정 건수
        this.settlementConfirmedCounter = Counter.builder("settlement.batch.confirmed")
            .description("Number of settlements confirmed by batch")
            .tag("batch", "settlement_confirmation")
            .register(meterRegistry);

        // 정산 조정 확정 건수
        this.adjustmentConfirmedCounter = Counter.builder("settlement.batch.adjustment_confirmed")
            .description("Number of settlement adjustments confirmed by batch")
            .tag("batch", "adjustment_confirmation")
            .register(meterRegistry);

        // 정산 생성 배치 실행 시간
        this.settlementCreationTimer = Timer.builder("settlement.batch.creation.duration")
            .description("Duration of settlement creation batch")
            .tag("batch", "settlement_creation")
            .register(meterRegistry);

        // 정산 확정 배치 실행 시간
        this.settlementConfirmationTimer = Timer.builder("settlement.batch.confirmation.duration")
            .description("Duration of settlement confirmation batch")
            .tag("batch", "settlement_confirmation")
            .register(meterRegistry);

        // 정산 조정 확정 배치 실행 시간
        this.adjustmentConfirmationTimer = Timer.builder("settlement.batch.adjustment.duration")
            .description("Duration of settlement adjustment confirmation batch")
            .tag("batch", "adjustment_confirmation")
            .register(meterRegistry);
    }

    public void incrementSettlementCreated(int count) {
        settlementCreatedCounter.increment(count);
    }

    public void incrementSettlementConfirmed(int count) {
        settlementConfirmedCounter.increment(count);
    }

    public void incrementAdjustmentConfirmed(int count) {
        adjustmentConfirmedCounter.increment(count);
    }

    /**
     * 배치 실패 기록
     *
     * tag key를 "batch"로 통일해서,
     * created/confirmed 타이머/카운터와 같은 차원으로 조회 가능하게 만듦
     */
    public void recordBatchFailure(String batchName) {
        Counter.builder("settlement.batch.failures")
            .description("Number of batch job failures")
            .tag("batch", batchName)
            .register(meterRegistry)
            .increment();
    }

    public void recordSettlementCreationTime(Duration duration) {
        settlementCreationTimer.record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void recordSettlementConfirmationTime(Duration duration) {
        settlementConfirmationTimer.record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void recordAdjustmentConfirmationTime(Duration duration) {
        adjustmentConfirmationTimer.record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Timer.Sample 기반 측정 (registry 파라미터 받을 필요 없음)
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopAndRecordSettlementCreation(Timer.Sample sample) {
        sample.stop(settlementCreationTimer);
    }

    public void stopAndRecordSettlementConfirmation(Timer.Sample sample) {
        sample.stop(settlementConfirmationTimer);
    }

    public void stopAndRecordAdjustmentConfirmation(Timer.Sample sample) {
        sample.stop(adjustmentConfirmationTimer);
    }
}
