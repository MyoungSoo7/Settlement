package github.lms.lemuel.settlement.adapter.in.web;

import github.lms.lemuel.settlement.adapter.in.web.response.SettlementResponse;
import github.lms.lemuel.settlement.application.port.in.GetSettlementUseCase;
import github.lms.lemuel.settlement.domain.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Settlement API Controller
 */
@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final GetSettlementUseCase getSettlementUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable Long id) {
        Settlement settlement = getSettlementUseCase.getSettlementById(id);
        return ResponseEntity.ok(SettlementResponse.from(settlement));
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<SettlementResponse> getSettlementByPaymentId(@PathVariable Long paymentId) {
        var settlements = getSettlementUseCase.getSettlementsByPaymentId(paymentId);
        if (settlements.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(SettlementResponse.from(settlements.get(0)));
    }
}
