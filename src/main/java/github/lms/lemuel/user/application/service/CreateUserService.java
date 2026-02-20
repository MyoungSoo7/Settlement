package github.lms.lemuel.user.application.service;

import github.lms.lemuel.user.application.port.in.CreateUserUseCase;
import github.lms.lemuel.user.application.port.out.ExistsUserPort;
import github.lms.lemuel.user.application.port.out.PasswordHashPort;
import github.lms.lemuel.user.application.port.out.SaveUserPort;
import github.lms.lemuel.user.domain.User;
import github.lms.lemuel.user.domain.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

    private final ExistsUserPort existsUserPort;
    private final PasswordHashPort passwordHashPort;
    private final SaveUserPort saveUserPort;

    @Override
    @Transactional
    public User createUser(CreateUserCommand command) {
        // 중복 체크
        if (existsUserPort.existsByEmail(command.email())) {
            throw new DuplicateEmailException(command.email());
        }

        // 비밀번호 해싱
        String hashedPassword = passwordHashPort.hash(command.rawPassword());

        // 도메인 생성
        User user = User.create(command.email(), hashedPassword);

        // 저장
        return saveUserPort.save(user);
    }
}
