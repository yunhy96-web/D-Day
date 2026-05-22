package com.flasharena.order.application;

import com.flasharena.order.domain.OutboxEvent;
import com.flasharena.order.infrastructure.OutboxRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Message Relayer: "order".outbox 의 READY 이벤트를 주기적으로 Redis Stream(XADD)으로 발행한다.
 * <p>각 틱은 자체 트랜잭션 안에서:
 *   1) READY 배치를 FOR UPDATE SKIP LOCKED 로 잠금 조회(중복 발행 방지),
 *   2) 건별 XADD,
 *   3) XADD 성공(RecordId 반환=스트림이 적재함) → PUBLISHED + published_at,
 *   4) 실패 → retry_count++ (상한 초과 시 FAILED=준-DLQ), 나머지는 READY 로 두고 다음 틱 재시도.
 * <p>⚠️ XADD 성공 = "스트림이 메시지를 받아들였다" 일 뿐, "소비자가 처리했다" 가 아니다.
 *   소비자 멱등성은 payment 도메인의 inbox(UNIQUE) 가 별도로 보장한다.
 */
@Component
public class OutboxRelayer {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayer.class);

    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final SimulationLogger simulationLogger;
    private final int batchSize;
    private final int maxRetry;
    private final String streamKey;

    public OutboxRelayer(OutboxRepository outboxRepository,
            StringRedisTemplate redisTemplate,
            SimulationLogger simulationLogger,
            @Value("${app.outbox.batch-size:100}") int batchSize,
            @Value("${app.outbox.max-retry:10}") int maxRetry,
            @Value("${app.stream.key:flasharena:order-events}") String streamKey) {
        this.outboxRepository = outboxRepository;
        this.redisTemplate = redisTemplate;
        this.simulationLogger = simulationLogger;
        this.batchSize = batchSize;
        this.maxRetry = maxRetry;
        this.streamKey = streamKey;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxRepository.findReadyBatchForUpdate(batchSize);
        if (batch.isEmpty()) {
            return;
        }

        int published = 0;
        int failed = 0;
        for (OutboxEvent event : batch) {
            try {
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("eventId", event.getId().toString());
                fields.put("eventType", event.getEventType());
                fields.put("aggregateType", event.getAggregateType());
                fields.put("aggregateId", event.getAggregateId().toString());
                fields.put("payload", event.getPayload());

                RecordId recordId = redisTemplate.opsForStream()
                        .add(StreamRecords.mapBacked(fields).withStreamKey(streamKey));

                if (recordId != null) {
                    // XADD ACK = 스트림이 적재함(소비 완료가 아님). published_at 기록 후 PUBLISHED 전이.
                    outboxRepository.markPublished(event.getId(), OffsetDateTime.now());
                    published++;
                } else {
                    bumpRetryOrFail(event);
                    failed++;
                }
            } catch (RuntimeException e) {
                // 발행 실패: 마킹하지 않고 retry_count 만 올린다(상한 초과 시 FAILED). 다음 틱에서 재시도.
                bumpRetryOrFail(event);
                failed++;
                log.warn("아웃박스 발행 실패 eventId={} retry={} : {}",
                        event.getId(), event.getRetryCount() + 1, e.getMessage());
            }
        }

        if (published > 0 || failed > 0) {
            log.info("아웃박스 릴레이: 발행 {}건, 실패 {}건 → Stream '{}'", published, failed, streamKey);
            if (published > 0) {
                simulationLogger.summary("outbox-relayer",
                        String.format("📨 아웃박스 릴레이: %d건 발행 완료", published));
            }
        }
    }

    /** 재시도 상한 초과 시 FAILED(준-DLQ), 아니면 retry_count 만 증가. */
    private void bumpRetryOrFail(OutboxEvent event) {
        if (event.getRetryCount() + 1 >= maxRetry) {
            outboxRepository.markFailed(event.getId());
            log.warn("아웃박스 재시도 상한 초과 → FAILED(준-DLQ) eventId={} retry={}",
                    event.getId(), event.getRetryCount() + 1);
        } else {
            outboxRepository.incrementRetry(event.getId());
        }
    }
}
