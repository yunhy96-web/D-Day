package com.flasharena.order.infrastructure;

import com.flasharena.order.domain.OutboxEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * READY 이벤트를 오래된 순으로 limit 만큼 가져온다.
     * FOR UPDATE SKIP LOCKED → 여러 릴레이어 틱/인스턴스가 동시에 폴링해도
     * 이미 락 잡힌 행은 건너뛰어 같은 이벤트를 중복 발행하지 않는다(at-least-once + 중복 최소화).
     * (native query: JPQL 은 SKIP LOCKED 를 직접 표현 못함.)
     */
    @Query(value = """
            SELECT * FROM "order".outbox
            WHERE status = 'READY'
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findReadyBatchForUpdate(@Param("limit") int limit);

    /** XADD 성공 → PUBLISHED + published_at 기록. */
    @Modifying
    @Query("""
            UPDATE OutboxEvent o
            SET o.status = com.flasharena.order.domain.OutboxEvent.STATUS_PUBLISHED, o.publishedAt = :publishedAt
            WHERE o.id = :id
            """)
    void markPublished(@Param("id") UUID id, @Param("publishedAt") OffsetDateTime publishedAt);

    /** 발행 실패 → retry_count 증가만 (다음 틱에서 다시 READY 로 시도). */
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.retryCount = o.retryCount + 1 WHERE o.id = :id")
    void incrementRetry(@Param("id") UUID id);

    /** 재시도 상한 초과 → FAILED(준-DLQ) + retry_count 증가. */
    @Modifying
    @Query("""
            UPDATE OutboxEvent o
            SET o.status = com.flasharena.order.domain.OutboxEvent.STATUS_FAILED, o.retryCount = o.retryCount + 1
            WHERE o.id = :id
            """)
    void markFailed(@Param("id") UUID id);

    /** 시뮬레이터 리셋: outbox 잔여물 삭제 (재현 가능성 확보). 자체 트랜잭션. */
    @Transactional
    @Modifying
    @Query("DELETE FROM OutboxEvent o")
    void deleteAllOutbox();
}
