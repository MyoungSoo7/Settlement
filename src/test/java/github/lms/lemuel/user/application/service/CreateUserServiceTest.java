package github.lms.lemuel.user.application.service;

import github.lms.lemuel.user.application.port.in.CreateUserUseCase.CreateUserCommand;
import github.lms.lemuel.user.application.port.out.ExistsUserPort;
import github.lms.lemuel.user.application.port.out.PasswordHashPort;
import github.lms.lemuel.user.application.port.out.SaveUserPort;
import github.lms.lemuel.user.domain.User;
import github.lms.lemuel.user.domain.UserRole;
import github.lms.lemuel.user.domain.exception.DuplicateEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserService 단위 테스트")
class CreateUserServiceTest {

    @Mock
    private ExistsUserPort existsUserPort;

    @Mock
    private PasswordHashPort passwordHashPort;

    @Mock
    private SaveUserPort saveUserPort;

    @InjectMocks
    private CreateUserService createUserService;

    @Test
    @DisplayName("회원가입 성공")
    void createUser_Success() {
        // given
        String email = "test@example.com";
        String rawPassword = "password123";
        String hashedPassword = "hashed_password";

        CreateUserCommand command = new CreateUserCommand(email, rawPassword);

        given(existsUserPort.existsByEmail(email)).willReturn(false);
        given(passwordHashPort.hash(rawPassword)).willReturn(hashedPassword);

        User savedUser = new User(1L, email, hashedPassword, UserRole.USER,
                LocalDateTime.now(), LocalDateTime.now());
        given(saveUserPort.save(any(User.class))).willReturn(savedUser);

        // when
        User result = createUserService.createUser(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(UserRole.USER);

        verify(existsUserPort).existsByEmail(email);
        verify(passwordHashPort).hash(rawPassword);
        verify(saveUserPort).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void createUser_DuplicateEmail_ThrowsException() {
        // given
        String email = "duplicate@example.com";
        CreateUserCommand command = new CreateUserCommand(email, "password123");

        given(existsUserPort.existsByEmail(email)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> createUserService.createUser(command))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining(email);

        verify(existsUserPort).existsByEmail(email);
        verify(passwordHashPort, never()).hash(anyString());
        verify(saveUserPort, never()).save(any(User.class));
    }
}
