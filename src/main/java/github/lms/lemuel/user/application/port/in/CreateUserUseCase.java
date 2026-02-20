package github.lms.lemuel.user.application.port.in;

import github.lms.lemuel.user.domain.User;

/**
 * 회원 생성 UseCase (Inbound Port)
 */
public interface CreateUserUseCase {

    User createUser(CreateUserCommand command);

    record CreateUserCommand(String email, String rawPassword) {
        public CreateUserCommand {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalArgumentException("Password is required");
            }
        }
    }
}
