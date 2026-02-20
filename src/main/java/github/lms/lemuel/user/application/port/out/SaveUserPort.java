package github.lms.lemuel.user.application.port.out;

import github.lms.lemuel.user.domain.User;

/**
 * 사용자 저장 Outbound Port
 */
public interface SaveUserPort {

    User save(User user);
}
