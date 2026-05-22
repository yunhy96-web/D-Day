package com.flasharena.payment.infrastructure;

import com.flasharena.payment.application.PaymentConsumer;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

/**
 * 결제 소비자의 Redis Stream 구성.
 * <p>소비자 그룹(consumer group) + 수동 XACK 방식:
 *   - 시작 시 그룹을 MKSTREAM 으로 생성(스트림이 없어도 생성), 이미 있으면(BUSYGROUP) 무시.
 *   - StreamMessageListenerContainer 가 XREADGROUP 으로 메시지를 폴링.
 *   - 핸들러 성공 시에만 XACK → 미처리/오류 메시지는 PEL 에 남아 재전달(at-least-once).
 * Pub/Sub 가 아니라 Streams 를 쓰는 이유: 그룹/ACK/미확인 메시지 재전달이 필요하기 때문.
 */
@Configuration
public class PaymentStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentStreamConfig.class);

    private final String streamKey;
    private final String group;
    private final String consumer;

    public PaymentStreamConfig(
            @Value("${app.stream.key:flasharena:order-events}") String streamKey,
            @Value("${app.stream.group:payment-group}") String group,
            @Value("${app.stream.consumer:payment-1}") String consumer) {
        this.streamKey = streamKey;
        this.group = group;
        this.consumer = consumer;
    }

    /** 스트림이 없으면 만들고(MKSTREAM), 그룹을 생성한다. 이미 있으면(BUSYGROUP) 그대로 둔다. */
    private void ensureGroup(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), group);
            log.info("소비자 그룹 생성 group='{}' stream='{}'", group, streamKey);
        } catch (Exception e) {
            // BUSYGROUP(이미 존재) 은 정상 경로 — 조용히 통과. 그 외는 경고만.
            String msg = e.getMessage();
            if (msg != null && msg.contains("BUSYGROUP")) {
                log.info("소비자 그룹 이미 존재 group='{}' (재사용)", group);
            } else {
                log.warn("소비자 그룹 생성 중 예외(무시 가능할 수 있음) group='{}': {}", group, msg);
            }
        }
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> paymentStreamContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redisTemplate,
            PaymentConsumer paymentConsumer) {

        ensureGroup(redisTemplate);

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // autoAck=false → 핸들러 성공 시에만 수동 XACK. 실패/예외 메시지는 PEL 에 남아 재전달된다.
        StreamListener<String, MapRecord<String, String, String>> listener = message -> {
            Map<String, String> fields = message.getValue();
            try {
                boolean processed = paymentConsumer.handle(fields);
                if (processed) {
                    redisTemplate.opsForStream().acknowledge(streamKey, group, message.getId());
                }
            } catch (RuntimeException e) {
                // 비-중복 오류: XACK 하지 않는다(PEL 잔류 → 재전달). 로그만 남긴다.
                log.warn("결제 소비 실패(미-XACK, 재전달 대상) id={} : {}", message.getId(), e.getMessage());
            }
        };

        container.receive(
                Consumer.from(group, consumer),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                listener);

        container.start();
        return container;
    }
}
