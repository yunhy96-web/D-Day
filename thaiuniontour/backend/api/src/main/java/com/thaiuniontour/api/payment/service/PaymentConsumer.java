package com.thaiuniontour.api.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thaiuniontour.domain.payment.model.PaymentHistory;
import com.thaiuniontour.domain.payment.repository.PaymentHistoryRepository;
import com.thaiuniontour.domain.reservation.event.ReservationConfirmed;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 멱등 소비자: RESERVATION_CONFIRMED 이벤트를 결제이력으로 멱등 적재.
 * 멱등성: 1) existsByReservationId 빠른 경로, 2) INSERT 시 UNIQUE 위반이면 중복으로 간주(진짜 방어선).
 * 결제 도메인은 reservation 스키마/outbox를 조회하지 않는다(Redis 메시지만).
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
     * 한 메시지 멱등 처리.
     * @return true = 처리(또는 중복/무시) 완료 → 호출부 XACK. 예외 throw = 재전달 대상.
     */
    @Transactional
    public boolean handle(Map<String, String> fields) {
        String eventType = fields.get("eventType");
        if (!ReservationConfirmed.TYPE.equals(eventType)) {
            return true; // 결제와 무관한 이벤트(취소 등)는 무시하고 ACK
        }
        String payloadJson = fields.get("payload");
        if (payloadJson == null) {
            log.warn("payload 없는 메시지 스킵: {}", fields);
            return true;
        }

        UUID reservationId;
        UUID memberId;
        long amount;
        String method;
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            reservationId = UUID.fromString(node.get("reservationId").asText());
            memberId = node.hasNonNull("memberId") ? UUID.fromString(node.get("memberId").asText()) : null;
            amount = node.has("amount") ? node.get("amount").asLong() : 0L;
            method = node.hasNonNull("paymentMethod") ? node.get("paymentMethod").asText() : "BANK_TRANSFER";
        } catch (Exception e) {
            log.warn("payload 파싱 실패 스킵: {} ({})", payloadJson, e.getMessage());
            return true; // 독이 든 메시지 제거
        }

        // 1) 빠른 경로
        if (paymentHistoryRepository.existsByReservationId(reservationId)) {
            log.info("이미 처리된 예약, 스킵 reservationId={}", reservationId);
            return true;
        }
        // 2) INSERT (UNIQUE 위반 = 동시 중복 → 스킵)
        try {
            paymentHistoryRepository.save(PaymentHistory.paid(reservationId, memberId, method, amount));
            log.info("결제 처리 완료 reservationId={} amount={}", reservationId, amount);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("중복 이벤트(UNIQUE 충돌), 스킵 reservationId={}", reservationId);
            return true;
        }
    }
}
