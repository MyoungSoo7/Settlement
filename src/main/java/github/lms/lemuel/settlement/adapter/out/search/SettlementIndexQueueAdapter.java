package github.lms.lemuel.settlement.adapter.out.search;

import github.lms.lemuel.settlement.application.port.out.EnqueueFailedIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 인덱싱 실패 시 재시도 큐 추가 Adapter (Outbound Adapter)
 * TODO: SettlementIndexQueueService를 settlement 모듈로 이동 후 연결
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true", matchIfMissing = false)
public class SettlementIndexQueueAdapter implements EnqueueFailedIndexPort {

    // TODO: SettlementIndexQueueService 주입
    // private final SettlementIndexQueueService queueService;

    @Override
    public void enqueueForRetry(Long settlementId, String operation) {
        log.info("재시도 큐 추가: settlementId={}, operation={}", settlementId, operation);

        // TODO: queueService.enqueue(settlementId, operation);

        // 임시: 로그만 출력
        log.warn("재시도 큐 서비스가 아직 이동되지 않음. settlementId={}", settlementId);
    }
}
