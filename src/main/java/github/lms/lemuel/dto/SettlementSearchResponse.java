package github.lms.lemuel.dto;

import github.lms.lemuel.search.SettlementSearchDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSearchResponse {

    // 검색 결과
    private List<SettlementSearchDocument> settlements;

    // 페이징 정보
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    // 집계 데이터
    private AggregationData aggregations;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationData {
        // 금액별 집계
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;

        // 상태별 집계
        private Map<String, Long> countByStatus;

        // 환불 여부 집계
        private Long refundedCount;
        private Long nonRefundedCount;

        // 기간별 통계
        private Map<String, Long> countByDate;
    }
}
