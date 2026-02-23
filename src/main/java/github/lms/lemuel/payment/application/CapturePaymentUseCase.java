package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.PaymentDomain;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.application.port.in.CapturePaymentPort;
import github.lms.lemuel.payment.application.port.out.LoadPaymentPort;
import github.lms.lemuel.payment.application.port.out.PgClientPort;
import github.lms.lemuel.payment.application.port.out.PublishEventPort;
import github.lms.lemuel.payment.application.port.out.SavePaymentPort;
import github.lms.lemuel.payment.application.port.out.UpdateOrderStatusPort;
import github.lms.lemuel.settlement.application.port.in.CreateSettlementFromPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CapturePaymentUseCase implements CapturePaymentPort {

    private static final Logger log = LoggerFactory.getLogger(CapturePaymentUseCase.class);

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PgClientPort pgClientPort;
    private final UpdateOrderStatusPort updateOrderStatusPort;
    private final PublishEventPort publishEventPort;
    private final CreateSettlementFromPaymentUseCase createSettlementFromPaymentUseCase;

    public CapturePaymentUseCase(LoadPaymentPort loadPaymentPort,
                                 SavePaymentPort savePaymentPort,
                                 PgClientPort pgClientPort,
                                 UpdateOrderStatusPort updateOrderStatusPort,
                                 PublishEventPort publishEventPort,
                                 CreateSettlementFromPaymentUseCase createSettlementFromPaymentUseCase) {
        this.loadPaymentPort = loadPaymentPort;
        this.savePaymentPort = savePaymentPort;
        this.pgClientPort = pgClientPort;
        this.updateOrderStatusPort = updateOrderStatusPort;
        this.publishEventPort = publishEventPort;
        this.createSettlementFromPaymentUseCase = createSettlementFromPaymentUseCase;
    }

    @Override
    public PaymentDomain capturePayment(Long paymentId) {
        PaymentDomain paymentDomain = loadPaymentPort.loadById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Call external PG to capture
        pgClientPort.capture(paymentDomain.getPgTransactionId(), paymentDomain.getAmount());

        // Domain logic
        paymentDomain.capture();

        // Save payment
        PaymentDomain savedPaymentDomain = savePaymentPort.save(paymentDomain);

        // Update order status to PAID
        updateOrderStatusPort.updateOrderStatus(savedPaymentDomain.getOrderId(), "PAID");

        // Publish event
        publishEventPort.publishPaymentCaptured(savedPaymentDomain.getId(), savedPaymentDomain.getOrderId());

        // ========== 정산 자동 생성 (핵심!) ==========
        try {
            createSettlementFromPaymentUseCase.createSettlementFromPayment(
                savedPaymentDomain.getId(),
                savedPaymentDomain.getOrderId(),
                savedPaymentDomain.getAmount()
            );
            log.info("Settlement created automatically for payment. paymentId={}", savedPaymentDomain.getId());
        } catch (Exception e) {
            log.error("Failed to create settlement for payment. paymentId={}", savedPaymentDomain.getId(), e);
            // 정산 생성 실패 시에도 결제는 정상 처리 (비동기로 재시도 가능)
        }

        return savedPaymentDomain;
    }
}
