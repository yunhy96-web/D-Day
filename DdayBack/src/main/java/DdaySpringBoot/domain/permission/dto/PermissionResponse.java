package DdaySpringBoot.domain.permission.dto;

import DdaySpringBoot.domain.permission.domain.RoleArticlePermission;
import lombok.Getter;

@Getter
public class PermissionResponse {
    private final String role;
    private final String articleType;
    private final Boolean canRead;
    private final Boolean canWrite;

    public PermissionResponse(RoleArticlePermission permission) {
        this.role = permission.getRole();
        this.articleType = permission.getArticleType();
        this.canRead = permission.getCanRead();
        this.canWrite = permission.getCanWrite();
    }
}
