package github.lms.lemuel.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for PaymentJpaEntity
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByOrderId(Long orderId);

    /**
     * 특정 기간 동안 캡처된 결제 조회 (정산 생성용)
     * @param startDateTime 시작 시간
     * @param endDateTime 종료 시간
     * @param status 결제 상태
     * @return 캡처된 결제 목록
     */
    List<PaymentJpaEntity> findByCapturedAtBetweenAndStatus(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String status);
}
