package com.flasharena.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "order".product 매핑 엔티티.
 * quantity(재고) 가 동시성 차감의 표적이며, SYNC 모드에서 음수까지 깨질 수 있다.
 * created_at/updated_at 은 DB default(now()) 가 채우므로 매핑만 하고 쓰기는 막는다.
 */
@Entity
@Table(name = "product", schema = "order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private long price;

    // 재고. SYNC 모드에선 lost-update 로 음수까지 깨지는 것을 시연한다.
    @Column(name = "quantity", nullable = false)
    private int quantity;

    // 낙관적 락 확장 대비 컬럼 (Phase 3 기본 모드에선 미사용).
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private Product(String name, long price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    /** 재고를 지정 수량으로 리셋한다 (각 시뮬레이션 시작 전 호출). */
    public void resetQuantity(int initialStock) {
        this.quantity = initialStock;
    }

    /** 재고 1 차감. 가드 없이 단순 차감 — SYNC 모드의 lost-update/oversell 을 그대로 노출한다. */
    public void decrease() {
        this.quantity = this.quantity - 1;
    }
}
