package github.lms.lemuel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementApprovalRequest {

    @NotBlank(message = "반려 사유는 필수입니다")
    private String reason;
}
