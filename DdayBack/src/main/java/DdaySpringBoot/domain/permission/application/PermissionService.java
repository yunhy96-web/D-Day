package DdaySpringBoot.domain.permission.application;

import DdaySpringBoot.domain.permission.domain.RoleArticlePermission;
import DdaySpringBoot.domain.permission.domain.RoleArticlePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final RoleArticlePermissionRepository permissionRepository;

    /**
     * 해당 권한으로 특정 게시글 타입을 읽을 수 있는지 확인
     */
    public boolean canRead(String role, String articleType) {
        return permissionRepository.findByRoleAndArticleType(role, articleType)
                .map(RoleArticlePermission::getCanRead)
                .orElse(false);
    }

    /**
     * 해당 권한으로 특정 게시글 타입을 작성할 수 있는지 확인
     */
    public boolean canWrite(String role, String articleType) {
        return permissionRepository.findByRoleAndArticleType(role, articleType)
                .map(RoleArticlePermission::getCanWrite)
                .orElse(false);
    }

    /**
     * 해당 권한으로 읽을 수 있는 게시글 타입 목록 조회
     */
    public List<String> getReadableArticleTypes(String role) {
        return permissionRepository.findByRoleAndCanReadTrue(role).stream()
                .map(RoleArticlePermission::getArticleType)
                .toList();
    }

    /**
     * 해당 권한으로 작성할 수 있는 게시글 타입 목록 조회
     */
    public List<String> getWritableArticleTypes(String role) {
        return permissionRepository.findByRoleAndCanWriteTrue(role).stream()
                .map(RoleArticlePermission::getArticleType)
                .toList();
    }

    /**
     * 해당 권한의 모든 권한 정보 조회
     */
    public List<RoleArticlePermission> getPermissionsByRole(String role) {
        return permissionRepository.findByRole(role);
    }
}
