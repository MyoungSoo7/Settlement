package github.lms.lemuel.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class SettlementSearchRequest {

    // 기간 범위 검색
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    // 주문자명 (정확한 키워드 매칭)
    private String ordererName;

    // 환불 여부
    private Boolean isRefunded;

    // 상품명 (전문 검색)
    private String productName;

    // 상태 필터
    private String status;

    // 페이징
    private int page = 0;
    private int size = 20;

    // 정렬
    private String sortBy = "orderCreatedAt";
    private String sortDirection = "DESC";
}
