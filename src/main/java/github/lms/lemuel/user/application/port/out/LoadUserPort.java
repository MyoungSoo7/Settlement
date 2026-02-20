package github.lms.lemuel.user.application.port.out;

import github.lms.lemuel.user.domain.User;

import java.util.Optional;

/**
 * 사용자 조회 Outbound Port
 */
public interface LoadUserPort {

    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);
}
