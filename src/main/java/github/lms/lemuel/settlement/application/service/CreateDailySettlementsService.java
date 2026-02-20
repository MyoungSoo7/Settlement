package github.lms.lemuel.settlement.application.service;

import github.lms.lemuel.settlement.application.port.in.CreateDailySettlementsUseCase;
import github.lms.lemuel.settlement.application.port.out.LoadCapturedPaymentsPort;
import github.lms.lemuel.settlement.application.port.out.LoadCapturedPaymentsPort.CapturedPaymentInfo;
import github.lms.lemuel.settlement.application.port.out.LoadSettlementPort;
import github.lms.lemuel.settlement.application.port.out.PublishSettlementEventPort;
import github.lms.lemuel.settlement.application.port.out.SaveSettlementPort;
import github.lms.lemuel.settlement.domain.Settlement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 일일 정산 생성 서비스
 * Spring Batch에 의존하지 않는 순수 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDailySettlementsService implements CreateDailySettlementsUseCase {

    private final LoadCapturedPaymentsPort loadCapturedPaymentsPort;
    private final LoadSettlementPort loadSettlementPort;
    private final SaveSettlementPort saveSettlementPort;
    private final PublishSettlementEventPort publishSettlementEventPort;

    @Override
    @Transactional
    public CreateSettlementResult createDailySettlements(CreateSettlementCommand command) {
        LocalDateTime startOfDay = command.targetDate().atStartOfDay();
        LocalDateTime endOfDay = command.targetDate().atTime(LocalTime.MAX);

        log.info("정산 생성 시작: targetDate={}, range={} ~ {}",
                command.targetDate(), startOfDay, endOfDay);

        // 1. 대상 결제 조회
        List<CapturedPaymentInfo> capturedPayments =
                loadCapturedPaymentsPort.findCapturedPaymentsBetween(startOfDay, endOfDay);

        int totalPayments = capturedPayments.size();
        int createdCount = 0;
        List<Long> createdSettlementIds = new ArrayList<>();

        // 2. 정산 생성
        for (CapturedPaymentInfo paymentInfo : capturedPayments) {
            // 중복 정산 체크
            if (loadSettlementPort.findByPaymentId(paymentInfo.paymentId()).isPresent()) {
                log.debug("정산이 이미 존재함: paymentId={}", paymentInfo.paymentId());
                continue;
            }

            // 도메인 엔티티 생성
            Settlement settlement = Settlement.create(
                    paymentInfo.paymentId(),
                    paymentInfo.orderId(),
                    paymentInfo.getSettlementAmount(),
                    command.targetDate()
            );

            Settlement saved = saveSettlementPort.save(settlement);
            createdSettlementIds.add(saved.getId());
            createdCount++;
        }

        log.info("정산 생성 완료: createdCount={}, totalPayments={}", createdCount, totalPayments);

        // 3. 이벤트 발행 (Elasticsearch 인덱싱 등)
        if (!createdSettlementIds.isEmpty()) {
            publishSettlementEventPort.publishSettlementCreatedEvent(createdSettlementIds);
        }

        return new CreateSettlementResult(createdCount, totalPayments);
    }
}
