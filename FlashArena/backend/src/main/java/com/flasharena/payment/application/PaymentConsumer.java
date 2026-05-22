package com.flasharena.payment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flasharena.payment.domain.PaymentHistory;
import com.flasharena.payment.infrastructure.PaymentHistoryRepository;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 멱등 소비자: Redis Stream 으로 전달된 ORDER_COMPLETED 이벤트를 결제 이력으로 멱등하게 적재한다.
 * <p>멱등성 보장 순서:
 *   1) existsByOrderId 빠른 경로 → 이미 처리됨이면 스킵(호출부가 XACK).
 *   2) INSERT 시도 → DB UNIQUE(uq_payment_history_order_id) 위반(DuplicateKey)이면 "이미 처리됨"으로 간주(스킵).
 *      ← 동시 중복 도착까지 막는 진짜 방어선.
 *   3) 정상 처리 → 호출부가 XACK.
 * <p>중복이 아닌 오류면 예외를 던져 XACK 하지 않게 한다(PEL 에 남아 재전달).
 * payment 는 order 스키마/아웃박스를 절대 조회하지 않는다.
 */
@Component
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ObjectMapper objectMapper;

    public PaymentConsumer(PaymentHistoryRepository paymentHistoryRepository, ObjectMapper objectMapper) {
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 한 메시지를 멱등하게 처리한다.
     *
     * @return true = 처리(또는 중복 스킵) 완료 → 호출부가 XACK 해야 함.
     *         예외 throw = 비-중복 오류 → XACK 하지 않음(재전달 대상).
     */
    @Transactional
    public boolean handle(Map<String, String> fields) {
        String payloadJson = fields.get("payload");
        if (payloadJson == null) {
            // 형식 불량 메시지는 재전달해도 소용없으므로 ACK 처리(독이 든 메시지 제거).
            log.warn("payload 없는 메시지 스킵: {}", fields);
            return true;
        }

        UUID orderId;
        long amount;
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            orderId = UUID.fromString(node.get("orderId").asText());
            amount = node.has("amount") ? node.get("amount").asLong() : 0L;
        } catch (Exception e) {
            log.warn("payload 파싱 실패 스킵: {} ({})", payloadJson, e.getMessage());
            return true;
        }

        // 1) 빠른 경로: 이미 처리된 주문이면 스킵.
        if (paymentHistoryRepository.existsByOrderId(orderId)) {
            log.info("이미 처리된 중복 이벤트, 스킵 orderId={}", orderId);
            return true;
        }

        // 2) INSERT 시도. UNIQUE 위반 시 동시 중복으로 간주하고 스킵(진짜 멱등 방어선).
        try {
            paymentHistoryRepository.save(PaymentHistory.paid(orderId, amount));
            // 3) 정상 처리.
            log.info("결제 처리 완료 orderId={} amount={}", orderId, amount);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("이미 처리된 중복 이벤트(UNIQUE 충돌), 스킵 orderId={}", orderId);
            return true;
        }
    }
}
