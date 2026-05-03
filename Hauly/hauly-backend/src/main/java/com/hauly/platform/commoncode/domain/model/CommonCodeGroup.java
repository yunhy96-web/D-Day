package com.hauly.platform.commoncode.domain.model;

import com.hauly.platform.support.exception.SystemProtectedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * CommonCodeGroup aggregate root.
 * Pragmatic DDD: JPA annotations on domain class.
 */
@Entity
@Table(name = "common_code_group")
public class CommonCodeGroup {

    @Id
    @Column(name = "group_code", length = 32)
    private String groupCode;

    @Column(name = "group_name_ko", nullable = false, length = 64)
    private String groupNameKo;

    @Column(name = "group_name_en", length = 64)
    private String groupNameEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CommonCodeGroup() {}

    private CommonCodeGroup(String groupCode, String groupNameKo, String groupNameEn,
                            String description, boolean system) {
        this.groupCode = groupCode;
        this.groupNameKo = groupNameKo;
        this.groupNameEn = groupNameEn;
        this.description = description;
        this.system = system;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public static CommonCodeGroup create(String groupCode, String groupNameKo, String groupNameEn,
                                         String description) {
        if (groupCode == null || groupCode.isBlank()) {
            throw new IllegalArgumentException("groupCode must not be blank");
        }
        if (groupNameKo == null || groupNameKo.isBlank()) {
            throw new IllegalArgumentException("groupNameKo must not be blank");
        }
        return new CommonCodeGroup(groupCode, groupNameKo, groupNameEn, description, false);
    }

    public void ensureNotSystem() {
        if (this.system) {
            throw new SystemProtectedException("Cannot modify system code group: " + this.groupCode);
        }
    }

    public void rename(String groupNameKo, String groupNameEn) {
        this.groupNameKo = groupNameKo;
        this.groupNameEn = groupNameEn;
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

    public String getGroupCode() { return groupCode; }
    public String getGroupNameKo() { return groupNameKo; }
    public String getGroupNameEn() { return groupNameEn; }
    public String getDescription() { return description; }
    public boolean isSystem() { return system; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
