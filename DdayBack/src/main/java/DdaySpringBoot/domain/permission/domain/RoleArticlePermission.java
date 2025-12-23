package DdaySpringBoot.domain.permission.domain;

import DdaySpringBoot.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "role_article_permissions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleArticlePermission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no", updatable = false)
    private Long no;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "article_type", nullable = false, length = 20)
    private String articleType;

    @Column(name = "can_read")
    private Boolean canRead;

    @Column(name = "can_write")
    private Boolean canWrite;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
