package com.thaiuniontour.domain.reservation.repository;

import com.thaiuniontour.domain.reservation.model.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * READY 배치를 잠금 조회. FOR UPDATE SKIP LOCKED → 멀티 인스턴스/동시 릴레이어 중복발행 방지.
     * 반드시 트랜잭션 안에서 호출(릴레이어 @Transactional).
     */
    @Query(value = """
        SELECT * FROM reservation.outbox
         WHERE status = 'READY'
         ORDER BY created_at
         LIMIT :batch
         FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findReadyBatchForUpdate(@Param("batch") int batch);
}
