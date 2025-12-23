package DdaySpringBoot.domain.common.domain;

import DdaySpringBoot.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "common_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no", updatable = false)
    private Long no;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label_ko", nullable = false, length = 100)
    private String labelKo;

    @Column(name = "label_en", nullable = false, length = 100)
    private String labelEn;

    @Column(name = "label_th", nullable = false, length = 100)
    private String labelTh;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Builder
    public CommonCode(String groupCode, String code, String labelKo, String labelEn, String labelTh, Integer sortOrder) {
        this.uuid = UUID.randomUUID().toString();
        this.groupCode = groupCode;
        this.code = code;
        this.labelKo = labelKo;
        this.labelEn = labelEn;
        this.labelTh = labelTh;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.isActive = true;
    }

    public String getLabelByLang(String lang) {
        return switch (lang) {
            case "ko" -> labelKo;
            case "th" -> labelTh;
            default -> labelEn;
        };
    }
}
