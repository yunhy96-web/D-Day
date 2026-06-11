package com.thaiuniontour.domain.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 (self-ref 트리 + type). 경량 컨텍스트.
 * parent_id는 같은 스키마 내 참조라 UUID 컬럼으로 단순 보관, 트리는 서비스에서 조립.
 */
@Entity
@Table(schema = "catalog", name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private int sort;

    @Column(name = "use_yn", nullable = false)
    private boolean useYn = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private Category(UUID parentId, CategoryType type, int level, String name, String slug, int sort) {
        this.id = UUID.randomUUID();
        this.parentId = parentId;
        this.type = type;
        this.level = level;
        this.name = name;
        this.slug = slug;
        this.sort = sort;
        this.useYn = true;
    }
}
