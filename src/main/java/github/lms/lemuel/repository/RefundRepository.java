package github.lms.lemuel.repository;

import github.lms.lemuel.domain.Refund;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 멱등성 체크: 동일 payment와 idempotency_key로 생성된 환불 조회
     */
    Optional<Refund> findByPaymentIdAndIdempotencyKey(Long paymentId, String idempotencyKey);

    /**
     * 특정 결제의 모든 환불 이력 조회
     */
    List<Refund> findByPaymentId(Long paymentId);

    /**
     * 특정 결제의 완료된 환불 금액 합계 조회
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentId = :paymentId AND r.status = 'COMPLETED'")
    java.math.BigDecimal sumCompletedRefundAmountByPaymentId(Long paymentId);
}
