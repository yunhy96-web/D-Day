package com.thaiuniontour.domain.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상품-카테고리 N:M 매핑 (테마/지역 동시 태깅). is_primary=대표분류(브레드크럼). */
@Entity
@Table(schema = "catalog", name = "product_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCategory {

    @EmbeddedId
    private Pk id;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    public ProductCategory(UUID productId, UUID categoryId, boolean primary) {
        this.id = new Pk(productId, categoryId);
        this.primary = primary;
    }

    public UUID getProductId() {
        return id.productId;
    }

    public UUID getCategoryId() {
        return id.categoryId;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Pk implements Serializable {
        @Column(name = "product_id", columnDefinition = "uuid")
        private UUID productId;

        @Column(name = "category_id", columnDefinition = "uuid")
        private UUID categoryId;

        public Pk(UUID productId, UUID categoryId) {
            this.productId = productId;
            this.categoryId = categoryId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(productId, pk.productId) && Objects.equals(categoryId, pk.categoryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, categoryId);
        }
    }
}
