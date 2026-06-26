package com.thaiuniontour.api.payment.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

/**
 * 결제 도메인의 Redis Stream 구독 설정.
 * consumer group으로 RESERVATION_CONFIRMED를 수신 → PaymentConsumer(멱등) → 성공 시 XACK.
 * 실패(예외) 시 XACK 안 함 → PEL에 남아 재전달(at-least-once).
 */
@Configuration
public class PaymentStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentStreamConfig.class);

    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate redisTemplate;
    private final PaymentConsumer paymentConsumer;
    private final String streamKey;
    private final String group;
    private final String consumerName;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    public PaymentStreamConfig(RedisConnectionFactory connectionFactory,
                               StringRedisTemplate redisTemplate,
                               PaymentConsumer paymentConsumer,
                               @Value("${app.stream.key:tut:reservation-events}") String streamKey,
                               @Value("${app.stream.consumer-group:payment-workers}") String group,
                               @Value("${app.stream.consumer-name:payment-1}") String consumerName) {
        this.connectionFactory = connectionFactory;
        this.redisTemplate = redisTemplate;
        this.paymentConsumer = paymentConsumer;
        this.streamKey = streamKey;
        this.group = group;
        this.consumerName = consumerName;
    }

    @PostConstruct
    void start() {
        createGroupIfAbsent();

        var options = StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();
        container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.receive(
                Consumer.from(group, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                this::onMessage);
        container.start();
        log.info("결제 스트림 구독 시작 key={} group={} consumer={}", streamKey, group, consumerName);
    }

    private void onMessage(MapRecord<String, String, String> record) {
        try {
            if (paymentConsumer.handle(record.getValue())) {
                redisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
            }
        } catch (Exception e) {
            // XACK 안 함 → PEL 잔류 후 재전달
            log.warn("결제 소비 실패(재전달 대상) id={} ({})", record.getId(), e.getMessage());
        }
    }

    /** 그룹 보장(없으면 생성, 스트림 없으면 MKSTREAM). 이미 있으면 무시. */
    private void createGroupIfAbsent() {
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) conn -> {
                try {
                    conn.streamCommands().xGroupCreate(
                            streamKey.getBytes(), group, ReadOffset.from("0-0"), true); // mkStream=true
                } catch (Exception ignore) {
                    // BUSYGROUP 등 이미 존재
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("consumer group 생성 확인 중 경고: {}", e.getMessage());
        }
    }

    @PreDestroy
    void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
