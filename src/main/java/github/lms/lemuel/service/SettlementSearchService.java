package github.lms.lemuel.service;

import github.lms.lemuel.dto.SettlementSearchRequest;
import github.lms.lemuel.dto.SettlementSearchResponse;
import github.lms.lemuel.search.SettlementSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SettlementSearchResponse search(SettlementSearchRequest request) {
        // Criteria 쿼리 생성
        Criteria criteria = buildCriteria(request);

        // 정렬 설정
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(direction, request.getSortBy())
        );

        // CriteriaQuery 빌드
        CriteriaQuery searchQuery = new CriteriaQuery(criteria, pageRequest);

        // 검색 실행
        SearchHits<SettlementSearchDocument> searchHits =
            elasticsearchOperations.search(searchQuery, SettlementSearchDocument.class);

        // 결과 추출
        List<SettlementSearchDocument> settlements = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // 집계 데이터는 별도 쿼리로 조회
        SettlementSearchResponse.AggregationData aggregations =
            calculateAggregations(criteria);

        // 페이징 정보 계산
        long totalElements = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

        return SettlementSearchResponse.builder()
                .settlements(settlements)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(request.getPage())
                .pageSize(request.getSize())
                .aggregations(aggregations)
                .build();
    }

    private Criteria buildCriteria(SettlementSearchRequest request) {
        Criteria criteria = new Criteria();

        // 1. 기간 범위 검색 (orderCreatedAt 기준)
        if (request.getStartDate() != null && request.getEndDate() != null) {
            criteria = criteria.and(new Criteria("orderCreatedAt")
                    .between(request.getStartDate(), request.getEndDate()));
        } else if (request.getStartDate() != null) {
            criteria = criteria.and(new Criteria("orderCreatedAt")
                    .greaterThanEqual(request.getStartDate()));
        } else if (request.getEndDate() != null) {
            criteria = criteria.and(new Criteria("orderCreatedAt")
                    .lessThanEqual(request.getEndDate()));
        }

        // 2. 주문자명 검색은 현재 Document에 없음 (추후 추가 필요)
        // User 정보를 Document에 추가해야 검색 가능

        // 3. 환불 여부 검색
        if (request.getIsRefunded() != null) {
            criteria = criteria.and(new Criteria("hasRefund")
                    .is(request.getIsRefunded()));
        }

        // 4. 결제 수단 검색 (Full-text search with Nori)
        if (request.getProductName() != null && !request.getProductName().isEmpty()) {
            criteria = criteria.and(new Criteria("paymentMethod")
                    .matches(request.getProductName()));
        }

        // 5. 정산 상태 필터
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            criteria = criteria.and(new Criteria("settlementStatus")
                    .is(request.getStatus()));
        }

        return criteria;
    }

    private SettlementSearchResponse.AggregationData calculateAggregations(Criteria criteria) {
        // 집계를 위한 별도 쿼리 실행
        CriteriaQuery aggQuery = new CriteriaQuery(criteria);
        SearchHits<SettlementSearchDocument> allHits =
            elasticsearchOperations.search(aggQuery, SettlementSearchDocument.class);

        List<SettlementSearchDocument> allSettlements = allHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // 수동 집계 계산
        BigDecimal totalAmount = allSettlements.stream()
                .map(SettlementSearchDocument::getSettlementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgAmount = allSettlements.isEmpty() ? BigDecimal.ZERO :
                totalAmount.divide(BigDecimal.valueOf(allSettlements.size()), 2, RoundingMode.HALF_UP);

        BigDecimal minAmount = allSettlements.stream()
                .map(SettlementSearchDocument::getSettlementAmount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxAmount = allSettlements.stream()
                .map(SettlementSearchDocument::getSettlementAmount)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 상태별 집계
        Map<String, Long> countByStatus = allSettlements.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSettlementStatus() != null ? s.getSettlementStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        // 환불 여부 집계
        long refundedCount = allSettlements.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHasRefund()))
                .count();

        long nonRefundedCount = allSettlements.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getHasRefund()))
                .count();

        // 날짜별 집계 (간단한 버전)
        Map<String, Long> countByDate = new HashMap<>();

        return SettlementSearchResponse.AggregationData.builder()
                .totalAmount(totalAmount)
                .averageAmount(avgAmount)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .countByStatus(countByStatus)
                .refundedCount(refundedCount)
                .nonRefundedCount(nonRefundedCount)
                .countByDate(countByDate)
                .build();
    }
}
