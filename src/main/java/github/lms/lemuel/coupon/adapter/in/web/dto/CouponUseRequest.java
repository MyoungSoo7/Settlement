package github.lms.lemuel.coupon.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CouponUseRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long orderId;
}