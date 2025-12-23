package DdaySpringBoot.domain.permission.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleArticlePermissionRepository extends JpaRepository<RoleArticlePermission, Long> {

    List<RoleArticlePermission> findByRole(String role);

    Optional<RoleArticlePermission> findByRoleAndArticleType(String role, String articleType);

    List<RoleArticlePermission> findByRoleAndCanReadTrue(String role);

    List<RoleArticlePermission> findByRoleAndCanWriteTrue(String role);
}
