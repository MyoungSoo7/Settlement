package github.lms.lemuel.coupon.application.service;

import github.lms.lemuel.coupon.application.port.out.LoadCouponPort;
import github.lms.lemuel.coupon.application.port.out.SaveCouponPort;
import github.lms.lemuel.coupon.application.port.in.CouponUseCase.ValidateResult;
import github.lms.lemuel.coupon.domain.Coupon;
import github.lms.lemuel.coupon.domain.CouponType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 단위 테스트")
class CouponServiceTest {

    @Mock
    private LoadCouponPort loadCouponPort;

    @Mock
    private SaveCouponPort saveCouponPort;

    @InjectMocks
    private CouponService couponService;

    @Nested
    @DisplayName("쿠폰 검증 테스트")
    class ValidateCouponTest {

        @Test
        @DisplayName("정액 쿠폰이 정상적으로 적용된다")
        void validateCoupon_Fixed_Success() {
            // given
            String code = "FIXED5000";
            Long userId = 1L;
            BigDecimal orderAmount = new BigDecimal("50000");
            Coupon coupon = Coupon.create(code, CouponType.FIXED, new BigDecimal("5000"), BigDecimal.ZERO, null, 100, null);
            coupon.setId(1L);

            given(loadCouponPort.findByCode(code)).willReturn(Optional.of(coupon));
            given(loadCouponPort.hasUserUsedCoupon(1L, userId)).willReturn(false);

            // when
            ValidateResult result = couponService.validateCoupon(code, userId, orderAmount);

            // then
            assertThat(result.valid()).isTrue();
            assertThat(result.discountAmount()).isEqualByComparingTo("5000");
            assertThat(result.finalAmount()).isEqualByComparingTo("45000");
        }

        @Test
        @DisplayName("정률 쿠폰이 정상적으로 적용된다")
        void validateCoupon_Percentage_Success() {
            // given
            String code = "DISC10";
            Long userId = 1L;
            BigDecimal orderAmount = new BigDecimal("50000");
            Coupon coupon = Coupon.create(code, CouponType.PERCENTAGE, new BigDecimal("10"), BigDecimal.ZERO, null, 100, null);
            coupon.setId(1L);

            given(loadCouponPort.findByCode(code)).willReturn(Optional.of(coupon));
            given(loadCouponPort.hasUserUsedCoupon(1L, userId)).willReturn(false);

            // when
            ValidateResult result = couponService.validateCoupon(code, userId, orderAmount);

            // then
            assertThat(result.valid()).isTrue();
            assertThat(result.discountAmount()).isEqualByComparingTo("5000");
            assertThat(result.finalAmount()).isEqualByComparingTo("45000");
        }

        @Test
        @DisplayName("할인 상한선이 정상적으로 적용된다")
        void validateCoupon_MaxDiscount_Applied() {
            // given
            String code = "DISC10_MAX1000";
            Long userId = 1L;
            BigDecimal orderAmount = new BigDecimal("50000"); // 10%면 5000원이지만 상한선이 1000원
            Coupon coupon = Coupon.create(code, CouponType.PERCENTAGE, new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("1000"), 100, null);
            coupon.setId(1L);

            given(loadCouponPort.findByCode(code)).willReturn(Optional.of(coupon));
            given(loadCouponPort.hasUserUsedCoupon(1L, userId)).willReturn(false);

            // when
            ValidateResult result = couponService.validateCoupon(code, userId, orderAmount);

            // then
            assertThat(result.valid()).isTrue();
            assertThat(result.discountAmount()).isEqualByComparingTo("1000");
            assertThat(result.finalAmount()).isEqualByComparingTo("49000");
        }

        @Test
        @DisplayName("이미 사용한 쿠폰은 적용되지 않는다")
        void validateCoupon_AlreadyUsed_Fails() {
            // given
            String code = "USED_COUPON";
            Long userId = 1L;
            BigDecimal orderAmount = new BigDecimal("50000");
            Coupon coupon = Coupon.create(code, CouponType.FIXED, new BigDecimal("5000"), BigDecimal.ZERO, null, 100, null);
            coupon.setId(1L);

            given(loadCouponPort.findByCode(code)).willReturn(Optional.of(coupon));
            given(loadCouponPort.hasUserUsedCoupon(1L, userId)).willReturn(true);

            // when
            ValidateResult result = couponService.validateCoupon(code, userId, orderAmount);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).isEqualTo("이미 사용한 쿠폰입니다.");
        }

        @Test
        @DisplayName("최소 주문 금액 미달 시 적용되지 않는다")
        void validateCoupon_MinOrderAmount_Fails() {
            // given
            String code = "MIN100000";
            Long userId = 1L;
            BigDecimal orderAmount = new BigDecimal("50000");
            Coupon coupon = Coupon.create(code, CouponType.FIXED, new BigDecimal("10000"), new BigDecimal("100000"), null, 100, null);
            coupon.setId(1L);

            given(loadCouponPort.findByCode(code)).willReturn(Optional.of(coupon));
            given(loadCouponPort.hasUserUsedCoupon(1L, userId)).willReturn(false);

            // when
            ValidateResult result = couponService.validateCoupon(code, userId, orderAmount);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).contains("최소 주문 금액");
        }

        @Test
        @DisplayName("만료된 쿠폰은 적용되지 않는다")
        void validateCoupon_Expired_Fails() {
            // given
            String code = "EXPIRED";
            Long userId = 1L;
            BigDecimal orderAmount = new BigDecimal("50000");
            // 1시간 전 만료
            Coupon coupon = Coupon.create(code, CouponType.FIXED, new BigDecimal("5000"), BigDecimal.ZERO, null, 100, java.time.LocalDateTime.now().minusHours(1));
            coupon.setId(1L);

            given(loadCouponPort.findByCode(code)).willReturn(Optional.of(coupon));
            given(loadCouponPort.hasUserUsedCoupon(1L, userId)).willReturn(false);

            // when
            ValidateResult result = couponService.validateCoupon(code, userId, orderAmount);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).isEqualTo("만료된 쿠폰입니다.");
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 테스트")
    class UseCouponTest {

        @Test
        @DisplayName("쿠폰 사용 시 사용 횟수가 증가하고 내역이 기록된다")
        void useCoupon_Success() {
            // given
            String code = "COUPON123";
            Long userId = 1L;
            Long orderId = 100L;
            Coupon coupon = Coupon.create(code, CouponType.FIXED, new BigDecimal("5000"), BigDecimal.ZERO, null, 100, null);
            coupon.setId(1L);

            given(loadCouponPort.findByCode(code)).willReturn(Optional.of(coupon));

            // when
            couponService.useCoupon(code, userId, orderId);

            // then
            assertThat(coupon.getUsedCount()).isEqualTo(1);
            org.mockito.Mockito.verify(saveCouponPort).save(coupon);
            org.mockito.Mockito.verify(saveCouponPort).recordUsage(1L, userId, orderId);
        }
    }

    @Nested
    @DisplayName("환불 할인 재계산 테스트")
    class RefundDiscountTest {

        @Test
        @DisplayName("정률 할인 부분 환불 시 할인 금액이 비례적으로 계산된다")
        void calculateDiscountForRefund_Percentage_Proportional() {
            // given
            // 100,000원 주문, 10% 쿠폰 -> 10,000원 할인.
            // 50,000원(원본 가격) 부분 환불 시.
            Coupon coupon = Coupon.create("C10", CouponType.PERCENTAGE, new BigDecimal("10"), BigDecimal.ZERO, null, 100, null);
            BigDecimal originalOrderAmount = new BigDecimal("100000");
            BigDecimal refundOriginalAmount = new BigDecimal("50000");

            // when
            BigDecimal refundDiscount = coupon.calculateDiscountForRefund(originalOrderAmount, refundOriginalAmount);

            // then
            // 10,000 * (50,000 / 100,000) = 5,000
            assertThat(refundDiscount).isEqualByComparingTo("5000");
        }

        @Test
        @DisplayName("정액 할인 부분 환불 시 할인 금액이 비례적으로 계산된다")
        void calculateDiscountForRefund_Fixed_Proportional() {
            // given
            // 100,000원 주문, 5,000원 정액 쿠폰 -> 5,000원 할인.
            // 40,000원(원본 가격) 부분 환불 시.
            Coupon coupon = Coupon.create("F5000", CouponType.FIXED, new BigDecimal("5000"), BigDecimal.ZERO, null, 100, null);
            BigDecimal originalOrderAmount = new BigDecimal("100000");
            BigDecimal refundOriginalAmount = new BigDecimal("40000");

            // when
            BigDecimal refundDiscount = coupon.calculateDiscountForRefund(originalOrderAmount, refundOriginalAmount);

            // then
            // 5,000 * (40,000 / 100,000) = 2,000
            assertThat(refundDiscount).isEqualByComparingTo("2000");
        }

        @Test
        @DisplayName("상한선이 적용된 경우에도 비례적으로 계산된다")
        void calculateDiscountForRefund_WithMaxDiscount_Proportional() {
            // given
            // 100,000원 주문, 10% 쿠폰 (10,000원), 상한선 6,000원 -> 6,000원 할인.
            // 50,000원(원본 가격) 부분 환불 시.
            Coupon coupon = Coupon.create("C10MAX6000", CouponType.PERCENTAGE, new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("6000"), 100, null);
            BigDecimal originalOrderAmount = new BigDecimal("100000");
            BigDecimal refundOriginalAmount = new BigDecimal("50000");

            // when
            BigDecimal refundDiscount = coupon.calculateDiscountForRefund(originalOrderAmount, refundOriginalAmount);

            // then
            // 6,000 * (50,000 / 100,000) = 3,000
            assertThat(refundDiscount).isEqualByComparingTo("3000");
        }
    }
}
