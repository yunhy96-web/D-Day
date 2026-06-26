package com.thaiuniontour.api.reservation.service;

import com.thaiuniontour.domain.reservation.model.OutboxEvent;
import com.thaiuniontour.domain.reservation.repository.OutboxRepository;
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
 * Message Relayer: reservation.outbox(READY)를 주기적으로 Redis Stream(XADD)으로 발행.
 * 각 틱은 자체 트랜잭션에서 FOR UPDATE SKIP LOCKED로 잠금조회 → XADD → PUBLISHED 전이.
 * XADD 성공 = 스트림 적재일 뿐, 소비완료가 아니다(소비자 멱등성은 payment inbox가 별도 보장).
 */
@Component
public class OutboxRelayer {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayer.class);

    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final int batchSize;
    private final int maxRetry;
    private final String streamKey;

    public OutboxRelayer(OutboxRepository outboxRepository,
                         StringRedisTemplate redisTemplate,
                         @Value("${app.outbox.batch-size:100}") int batchSize,
                         @Value("${app.outbox.max-retry:10}") int maxRetry,
                         @Value("${app.stream.key:tut:reservation-events}") String streamKey) {
        this.outboxRepository = outboxRepository;
        this.redisTemplate = redisTemplate;
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
                    event.markPublished(OffsetDateTime.now());
                    published++;
                } else {
                    event.markFailed(maxRetry);
                }
            } catch (Exception e) {
                log.warn("아웃박스 발행 실패 eventId={} ({})", event.getId(), e.getMessage());
                event.markFailed(maxRetry);
            }
            outboxRepository.save(event);
        }
        if (published > 0) {
            log.debug("아웃박스 {}건 발행", published);
        }
    }
}
