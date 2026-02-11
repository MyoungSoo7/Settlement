package github.lms.lemuel.controller;

import github.lms.lemuel.dto.SettlementSearchRequest;
import github.lms.lemuel.dto.SettlementSearchResponse;
import github.lms.lemuel.service.SettlementSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/settlements/search")
@RequiredArgsConstructor
public class SettlementSearchController {

    private final SettlementSearchService settlementSearchService;

    /**
     * 복합 검색 API
     *
     * 검색 조건:
     * - startDate, endDate: 기간 범위 검색
     * - ordererName: 주문자명 (정확한 키워드 매칭)
     * - isRefunded: 환불 여부
     * - productName: 상품명 (전문 검색)
     * - status: 정산 상태
     * - page, size: 페이징
     * - sortBy, sortDirection: 정렬
     *
     * 응답:
     * - settlements: 검색 결과 목록
     * - totalElements, totalPages, currentPage, pageSize: 페이징 정보
     * - aggregations: 금액별, 상태별 집계 데이터
     */
    @GetMapping
    public ResponseEntity<SettlementSearchResponse> search(
            @ModelAttribute SettlementSearchRequest request) {

        log.info("Settlement search request: ordererName={}, productName={}, isRefunded={}, startDate={}, endDate={}, page={}, size={}",
                request.getOrdererName(), request.getProductName(), request.getIsRefunded(),
                request.getStartDate(), request.getEndDate(), request.getPage(), request.getSize());

        SettlementSearchResponse response = settlementSearchService.search(request);

        log.info("Search completed: found {} results, totalPages={}",
                response.getTotalElements(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * POST 방식의 복합 검색 (JSON Body)
     */
    @PostMapping
    public ResponseEntity<SettlementSearchResponse> searchByPost(
            @RequestBody SettlementSearchRequest request) {

        log.info("Settlement search (POST) request: {}", request);

        SettlementSearchResponse response = settlementSearchService.search(request);

        return ResponseEntity.ok(response);
    }
}
