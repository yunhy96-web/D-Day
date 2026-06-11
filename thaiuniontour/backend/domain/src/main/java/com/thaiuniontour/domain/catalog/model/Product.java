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
 * 상품 (경량 컨텍스트). stock_type으로 재고형 구분.
 * search_tsv는 DB generated column이라 매핑하지 않는다(읽기 전용 검색용).
 */
@Entity
@Table(schema = "catalog", name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String summary;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_type", nullable = false)
    private StockType stockType;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "cancellation_policy_id", columnDefinition = "uuid")
    private UUID cancellationPolicyId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private Product(String name, String summary, String description, StockType stockType,
                    long basePrice, ProductStatus status, String thumbnailUrl, UUID cancellationPolicyId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.stockType = stockType != null ? stockType : StockType.LIMITED;
        this.basePrice = basePrice;
        this.status = status != null ? status : ProductStatus.DRAFT;
        this.thumbnailUrl = thumbnailUrl;
        this.cancellationPolicyId = cancellationPolicyId;
    }

    public void update(String name, String summary, String description, long basePrice,
                       ProductStatus status, String thumbnailUrl) {
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.basePrice = basePrice;
        this.status = status;
        this.thumbnailUrl = thumbnailUrl;
    }
}
