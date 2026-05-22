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
 * "order".orders 매핑 엔티티.
 * ⚠️ 클래스명을 {@code Order} 로 하면 JPQL 예약어 ORDER 와 충돌하므로 {@code OrderEntity} 로 둔다.
 * user_id 는 auth.users.id 를 "타입으로만" 참조(물리 FK 없음). product_id 는 같은 스키마 FK.
 */
@Entity
@Table(name = "orders", schema = "order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    // CREATED(성공) / FAILED(재고부족 등)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private OrderEntity(UUID userId, UUID productId, int quantity, String status) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }
}
