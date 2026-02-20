package github.lms.lemuel.user.application.port.out;

/**
 * 비밀번호 해싱 Outbound Port
 */
public interface PasswordHashPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
