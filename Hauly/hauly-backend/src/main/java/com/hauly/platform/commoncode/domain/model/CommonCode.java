package com.hauly.platform.commoncode.domain.model;

import com.hauly.platform.support.exception.SystemProtectedException;
import com.hauly.shared.kernel.Lang;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * CommonCode entity with composite PK (group_code, code).
 * Pragmatic DDD: JPA annotations on domain class.
 */
@Entity
@Table(name = "common_code")
@IdClass(CommonCodeId.class)
public class CommonCode {

    @Id
    @Column(name = "group_code", length = 32)
    private String groupCode;

    @Id
    @Column(name = "code", length = 32)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 64)
    private String nameKo;

    @Column(name = "name_en", length = 64)
    private String nameEn;

    @Column(name = "name_th", length = 64)
    private String nameTh;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attributes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CommonCode() {}

    private CommonCode(String groupCode, String code, String nameKo, String nameEn,
                       String nameTh, int sortOrder, boolean system) {
        this.groupCode = groupCode;
        this.code = code;
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.nameTh = nameTh;
        this.sortOrder = sortOrder;
        this.system = system;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public static CommonCode create(String groupCode, String code, String nameKo, String nameEn,
                                    String nameTh, int sortOrder) {
        if (groupCode == null || groupCode.isBlank()) throw new IllegalArgumentException("groupCode required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code required");
        if (nameKo == null || nameKo.isBlank()) throw new IllegalArgumentException("nameKo required");
        return new CommonCode(groupCode, code, nameKo, nameEn, nameTh, sortOrder, false);
    }

    public void updateNames(String nameKo, String nameEn, String nameTh) {
        if (this.system) {
            throw new SystemProtectedException("Code '" + code + "' is a system code and cannot be renamed");
        }
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.nameTh = nameTh;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public String pickName(Lang lang) {
        return switch (lang) {
            case EN -> nameEn != null ? nameEn : nameKo;
            case TH -> nameTh != null ? nameTh : nameKo;
            default -> nameKo;
        };
    }

    public String getGroupCode() { return groupCode; }
    public String getCode() { return code; }
    public String getNameKo() { return nameKo; }
    public String getNameEn() { return nameEn; }
    public String getNameTh() { return nameTh; }
    public int getSortOrder() { return sortOrder; }
    public boolean isSystem() { return system; }
    public boolean isActive() { return active; }
    public String getAttributes() { return attributes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
