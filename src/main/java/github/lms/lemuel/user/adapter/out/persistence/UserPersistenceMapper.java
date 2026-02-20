package github.lms.lemuel.user.adapter.out.persistence;

import github.lms.lemuel.user.domain.User;
import github.lms.lemuel.user.domain.UserRole;
import org.springframework.stereotype.Component;

/**
 * Domain <-> JpaEntity 매핑
 */
@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                UserRole.fromString(entity.getRole()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserJpaEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail());
        entity.setPassword(domain.getPasswordHash());
        entity.setRole(domain.getRole().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
