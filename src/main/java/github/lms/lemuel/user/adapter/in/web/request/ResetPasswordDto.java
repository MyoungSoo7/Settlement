package github.lms.lemuel.user.adapter.in.web.request;

public record ResetPasswordDto(
        String token,
        String newPassword
) {
}
