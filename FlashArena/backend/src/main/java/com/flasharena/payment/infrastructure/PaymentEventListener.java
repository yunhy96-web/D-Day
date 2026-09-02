package com.flasharena.payment.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flasharena.payment.application.PaymentConsumer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 결제 소비자의 Kafka 리스너.
 * <p>소비자 그룹 + 수동 오프셋 커밋(ack-mode=manual):
 *   - 핸들러가 정상 처리(또는 중복 스킵)하면 ack → 오프셋 커밋.
 *   - 예외를 던지면 커밋하지 않는다 → 컨테이너 기본 에러 핸들러가 같은 오프셋으로 되돌아가 재시도(at-least-once).
 * 파티션 키가 orderId 이므로 같은 주문의 이벤트는 순서대로 한 소비자에게 온다.
 * ponytail: 재시도 소진 시 기본 핸들러가 로그만 남기고 건너뛴다. 유실이 문제 되면 DeadLetterPublishingRecoverer 로 DLT 토픽에 보낸다.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);
    private static final TypeReference<Map<String, String>> FIELDS = new TypeReference<>() { };

    private final PaymentConsumer paymentConsumer;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(PaymentConsumer paymentConsumer, ObjectMapper objectMapper) {
        this.paymentConsumer = paymentConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic:flasharena.order-events}", groupId = "${app.kafka.group:payment-group}")
    public void onMessage(String value, Acknowledgment ack) {
        Map<String, String> fields;
        try {
            fields = objectMapper.readValue(value, FIELDS);
        } catch (Exception e) {
            // 형식 불량 메시지는 재시도해도 소용없으므로 커밋하고 버린다(독이 든 메시지 제거).
            log.warn("이벤트 파싱 실패 스킵: {} ({})", value, e.getMessage());
            ack.acknowledge();
            return;
        }
        try {
            if (paymentConsumer.handle(fields)) {
                ack.acknowledge();
            }
        } catch (RuntimeException e) {
            // 비-중복 오류: 커밋하지 않고 다시 던진다 → 에러 핸들러가 재시도.
            log.warn("결제 소비 실패(미커밋, 재시도 대상) eventId={} : {}", fields.get("eventId"), e.getMessage());
            throw e;
        }
    }
}
