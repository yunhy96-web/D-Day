package com.thaiuniontour.domain.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 날짜별 재고/가격. 선착순 동시성의 핵심 자원.
 * stock=NULL → 무제한(UNLIMITED). 차감은 리포지토리 원자 UPDATE로만(엔티티 setter 없음).
 */
@Entity
@Table(schema = "catalog", name = "product_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSchedule {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "product_id", columnDefinition = "uuid", nullable = false)
    private UUID productId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    /** NULL = 무제한. */
    private Integer stock;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    @Column(insertable = false, updatable = false)
    private long version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private ProductSchedule(UUID productId, LocalDate saleDate, Integer stock, long price, ScheduleStatus status) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.saleDate = saleDate;
        this.stock = stock;
        this.price = price;
        this.status = status != null ? status : ScheduleStatus.OPEN;
    }

    public boolean isUnlimited() {
        return stock == null;
    }
}
