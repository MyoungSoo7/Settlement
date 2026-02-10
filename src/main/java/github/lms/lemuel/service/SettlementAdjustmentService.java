package github.lms.lemuel.service;

import github.lms.lemuel.domain.Refund;
import github.lms.lemuel.domain.Settlement;
import github.lms.lemuel.domain.SettlementAdjustment;
import github.lms.lemuel.repository.SettlementAdjustmentRepository;
import github.lms.lemuel.repository.SettlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 정산 조정 서비스
 *
 * CONFIRMED 정산에 대한 환불 시 조정(Adjustment) 생성
 */
@Service
public class SettlementAdjustmentService {

    private static final Logger logger = LoggerFactory.getLogger(SettlementAdjustmentService.class);

    private final SettlementRepository settlementRepository;
    private final SettlementAdjustmentRepository adjustmentRepository;

    public SettlementAdjustmentService(SettlementRepository settlementRepository,
                                       SettlementAdjustmentRepository adjustmentRepository) {
        this.settlementRepository = settlementRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    /**
     * 환불 완료 시 정산 조정 생성
     *
     * @param refund 완료된 환불
     */
    @Transactional
    public void createAdjustmentForRefund(Refund refund) {
        // 중복 조정 방지
        Optional<SettlementAdjustment> existing = adjustmentRepository.findByRefundId(refund.getId());
        if (existing.isPresent()) {
            logger.debug("이미 조정이 존재함: refundId={}", refund.getId());
            return;
        }

        // 해당 payment의 settlement 조회
        Optional<Settlement> settlementOpt = settlementRepository.findByPaymentId(refund.getPaymentId());
        if (settlementOpt.isEmpty()) {
            logger.info("정산이 존재하지 않음 (아직 배치 미실행): paymentId={}", refund.getPaymentId());
            return;
        }

        Settlement settlement = settlementOpt.get();

        // PENDING 정산: 금액 직접 차감 (조정 생성 대신)
        if (settlement.getStatus() == Settlement.SettlementStatus.PENDING) {
            logger.info("PENDING 정산 금액 차감: settlementId={}, refundAmount={}",
                       settlement.getId(), refund.getAmount());
            settlement.setAmount(settlement.getAmount().subtract(refund.getAmount()));
            settlementRepository.save(settlement);
            return;
        }

        // CONFIRMED 정산: 조정 레코드 생성 (표준 패턴)
        if (settlement.getStatus() == Settlement.SettlementStatus.CONFIRMED) {
            SettlementAdjustment adjustment = new SettlementAdjustment();
            adjustment.setSettlementId(settlement.getId());
            adjustment.setRefundId(refund.getId());
            adjustment.setAmount(refund.getAmount().negate()); // 음수
            adjustment.setStatus(SettlementAdjustment.AdjustmentStatus.PENDING);
            adjustment.setAdjustmentDate(LocalDate.now());

            adjustmentRepository.save(adjustment);
            logger.info("정산 조정 생성: settlementId={}, refundId={}, amount={}",
                       settlement.getId(), refund.getId(), adjustment.getAmount());
        }
    }

    /**
     * 조정 확정 (배치에서 호출)
     *
     * @param adjustmentId 조정 ID
     */
    @Transactional
    public void confirmAdjustment(Long adjustmentId) {
        SettlementAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new RuntimeException("Adjustment not found: " + adjustmentId));

        if (adjustment.getStatus() == SettlementAdjustment.AdjustmentStatus.PENDING) {
            adjustment.setStatus(SettlementAdjustment.AdjustmentStatus.CONFIRMED);
            adjustmentRepository.save(adjustment);
            logger.info("정산 조정 확정: adjustmentId={}", adjustmentId);
        }
    }
}
