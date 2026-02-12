package github.lms.lemuel.controller;

import github.lms.lemuel.domain.Settlement;
import github.lms.lemuel.dto.SettlementApprovalRequest;
import github.lms.lemuel.dto.SettlementResponse;
import github.lms.lemuel.repository.SettlementRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 정산 관리 API
 * - 정산 승인/반려
 * - 정산 상세 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementRepository settlementRepository;

    /**
     * 정산 상세 조회
     * GET /api/settlements/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + id));

        return ResponseEntity.ok(new SettlementResponse(settlement));
    }

    /**
     * 정산 승인
     * POST /api/settlements/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<SettlementResponse> approveSettlement(
            @PathVariable Long id,
            Authentication authentication) {

        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + id));

        // 승인 가능 상태 체크
        if (settlement.getStatus() != Settlement.SettlementStatus.WAITING_APPROVAL
                && settlement.getStatus() != Settlement.SettlementStatus.CALCULATED) {
            throw new RuntimeException("Settlement cannot be approved. Current status: " + settlement.getStatus());
        }

        // 승인 처리
        settlement.setStatus(Settlement.SettlementStatus.APPROVED);
        settlement.setApprovedAt(LocalDateTime.now());

        // 인증된 사용자 ID 설정 (실제로는 User 엔티티에서 가져와야 함)
        if (authentication != null) {
            // TODO: 실제 사용자 ID로 변경
            settlement.setApprovedBy(1L);
        }

        Settlement saved = settlementRepository.save(settlement);
        log.info("Settlement approved: id={}, approvedBy={}, approvedAt={}",
                saved.getId(), saved.getApprovedBy(), saved.getApprovedAt());

        return ResponseEntity.ok(new SettlementResponse(saved));
    }

    /**
     * 정산 반려
     * POST /api/settlements/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<SettlementResponse> rejectSettlement(
            @PathVariable Long id,
            @Valid @RequestBody SettlementApprovalRequest request,
            Authentication authentication) {

        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + id));

        // 반려 가능 상태 체크
        if (settlement.getStatus() != Settlement.SettlementStatus.WAITING_APPROVAL
                && settlement.getStatus() != Settlement.SettlementStatus.CALCULATED) {
            throw new RuntimeException("Settlement cannot be rejected. Current status: " + settlement.getStatus());
        }

        // 반려 처리
        settlement.setStatus(Settlement.SettlementStatus.REJECTED);
        settlement.setRejectedAt(LocalDateTime.now());
        settlement.setRejectionReason(request.getReason());

        // 인증된 사용자 ID 설정
        if (authentication != null) {
            settlement.setRejectedBy(1L);
        }

        Settlement saved = settlementRepository.save(settlement);
        log.info("Settlement rejected: id={}, rejectedBy={}, reason={}",
                saved.getId(), saved.getRejectedBy(), saved.getRejectionReason());

        return ResponseEntity.ok(new SettlementResponse(saved));
    }
}
