package com.flasharena.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * payment.payment_history 매핑 엔티티 (소비자 멱등성 = Inbox).
 * order_id 에 DB UNIQUE(uq_payment_history_order_id) 가 걸려 있어 같은 주문은 한 번만 결제 처리된다.
 * → exists() 선체크는 빠른 경로일 뿐, 동시 중복 도착 시 진짜 방어선은 이 UNIQUE 제약(DuplicateKey).
 * payment 도메인은 order 스키마/아웃박스를 절대 조회하지 않고 Redis Stream 만 구독한다.
 */
@Entity
@Table(name = "payment_history", schema = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // "order".orders.id 를 타입으로만 참조 (물리 FK 없음). 멱등성 키.
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    private PaymentHistory(UUID orderId, long amount, String status) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
    }

    /** 결제 완료(PAID) 이력 생성 팩토리. */
    public static PaymentHistory paid(UUID orderId, long amount) {
        return new PaymentHistory(orderId, amount, "PAID");
    }
}
