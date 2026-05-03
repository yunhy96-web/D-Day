package com.hauly.platform.category.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * ProductCategory aggregate root mapped to product_category table.
 * field_schema stored as JSONB.
 */
@Entity
@Table(name = "product_category")
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name_key", nullable = false, length = 128)
    private String nameKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_schema", nullable = false, columnDefinition = "jsonb")
    private String fieldSchema;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProductCategory() {}

    private ProductCategory(String code, String nameKey, String fieldSchema, int sortOrder) {
        this.code = code;
        this.nameKey = nameKey;
        this.fieldSchema = fieldSchema;
        this.sortOrder = sortOrder;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public static ProductCategory create(String code, String nameKey, String fieldSchema, int sortOrder) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code required");
        if (nameKey == null || nameKey.isBlank()) throw new IllegalArgumentException("nameKey required");
        if (fieldSchema == null || fieldSchema.isBlank()) {
            fieldSchema = "{\"fields\":[]}";
        }
        return new ProductCategory(code, nameKey, fieldSchema, sortOrder);
    }

    public void updateSchema(String newFieldSchema) {
        this.fieldSchema = newFieldSchema;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Long getParentId() { return parentId; }
    public String getNameKey() { return nameKey; }
    public String getFieldSchema() { return fieldSchema; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
