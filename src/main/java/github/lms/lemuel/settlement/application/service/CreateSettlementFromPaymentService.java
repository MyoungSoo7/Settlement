package github.lms.lemuel.settlement.application.service;

import github.lms.lemuel.settlement.application.port.in.CreateSettlementFromPaymentUseCase;
import github.lms.lemuel.settlement.application.port.out.LoadSettlementPort;
import github.lms.lemuel.settlement.application.port.out.SaveSettlementPort;
import github.lms.lemuel.settlement.domain.Settlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 결제 완료 시 정산 자동 생성 서비스
 */
@Service
@Transactional
public class CreateSettlementFromPaymentService implements CreateSettlementFromPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateSettlementFromPaymentService.class);

    private final LoadSettlementPort loadSettlementPort;
    private final SaveSettlementPort saveSettlementPort;

    public CreateSettlementFromPaymentService(LoadSettlementPort loadSettlementPort,
                                              SaveSettlementPort saveSettlementPort) {
        this.loadSettlementPort = loadSettlementPort;
        this.saveSettlementPort = saveSettlementPort;
    }

    @Override
    public Settlement createSettlementFromPayment(Long paymentId, Long orderId, BigDecimal amount) {
        log.info("Creating settlement from payment. paymentId={}, orderId={}, amount={}", paymentId, orderId, amount);

        // Idempotency: 이미 정산이 존재하는지 확인
        Optional<Settlement> existingSettlement = loadSettlementPort.findByPaymentId(paymentId);
        if (existingSettlement.isPresent()) {
            log.info("Settlement already exists for paymentId={}. Returning existing settlement.", paymentId);
            return existingSettlement.get();
        }

        // 정산 생성 (D+7 정산일 기준)
        LocalDate settlementDate = LocalDate.now().plusDays(7);
        Settlement settlement = Settlement.createFromPayment(paymentId, orderId, amount, settlementDate);

        // 저장
        Settlement savedSettlement = saveSettlementPort.save(settlement);
        log.info("Settlement created successfully. settlementId={}, status={}",
                savedSettlement.getId(), savedSettlement.getStatus());

        return savedSettlement;
    }
}
