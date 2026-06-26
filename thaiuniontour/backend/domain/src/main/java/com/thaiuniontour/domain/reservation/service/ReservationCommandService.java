package com.thaiuniontour.domain.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thaiuniontour.common.domain.DomainEvent;
import com.thaiuniontour.common.error.ErrorCode;
import com.thaiuniontour.common.exception.BusinessException;
import com.thaiuniontour.domain.catalog.model.ProductSchedule;
import com.thaiuniontour.domain.catalog.model.ScheduleStatus;
import com.thaiuniontour.domain.catalog.port.StockDeductionStrategy;
import com.thaiuniontour.domain.catalog.repository.ProductScheduleRepository;
import com.thaiuniontour.domain.reservation.model.CancelReason;
import com.thaiuniontour.domain.reservation.model.OutboxEvent;
import com.thaiuniontour.domain.reservation.model.Reservation;
import com.thaiuniontour.domain.reservation.repository.OutboxRepository;
import com.thaiuniontour.domain.reservation.repository.ReservationRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 유스케이스(트랜잭션 경계). api(create/cancel)·admin(confirm) 공용.
 * 핵심: create는 재고 원자차감+예약INSERT를, confirm/cancel은 예약변경+OutboxINSERT를 같은 트랜잭션에 묶는다.
 */
@Service
public class ReservationCommandService {

    private static final String AGGREGATE_TYPE = "RESERVATION";
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray(); // 혼동문자 제외
    private final SecureRandom random = new SecureRandom();

    private final ProductScheduleRepository scheduleRepository;
    private final StockDeductionStrategy stockDeduction;
    private final ReservationRepository reservationRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final int deadlineHours;

    public ReservationCommandService(ProductScheduleRepository scheduleRepository,
                                     StockDeductionStrategy stockDeduction,
                                     ReservationRepository reservationRepository,
                                     OutboxRepository outboxRepository,
                                     ObjectMapper objectMapper,
                                     @Value("${app.reservation.payment-deadline-hours:48}") int deadlineHours) {
        this.scheduleRepository = scheduleRepository;
        this.stockDeduction = stockDeduction;
        this.reservationRepository = reservationRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.deadlineHours = deadlineHours;
    }

    /**
     * 예약 생성. 재고 원자차감 + 예약(PENDING) INSERT를 한 트랜잭션에 묶는다.
     * 동시에 호출돼도 재고 1개면 정확히 한 건만 성공(oversell 0%).
     */
    @Transactional
    public Reservation create(UUID memberId, UUID productScheduleId, int quantity, String paymentMethod,
                              String reserverName, String reserverEmail, String reserverPhone) {
        ProductSchedule schedule = scheduleRepository.findById(productScheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "일정을 찾을 수 없습니다."));
        if (schedule.getStatus() != ScheduleStatus.OPEN) {
            throw new BusinessException(ErrorCode.RESERVATION_SOLD_OUT, "예약할 수 없는 일정입니다.");
        }

        // 재고 원자 차감(강한 일관성 가드). 실패 = 매진.
        if (!stockDeduction.deduct(productScheduleId, quantity)) {
            throw new BusinessException(ErrorCode.RESERVATION_SOLD_OUT, "해당 날짜는 매진되었습니다.");
        }

        long amount = schedule.getPrice() * quantity;
        OffsetDateTime deadline = OffsetDateTime.now().plusHours(deadlineHours);
        Reservation reservation = Reservation.create(
                memberId, schedule.getProductId(), productScheduleId, schedule.getSaleDate(),
                quantity, amount, paymentMethod,
                reserverName, reserverEmail, reserverPhone,
                generateReservationNo(), deadline);
        return reservationRepository.save(reservation);
    }

    /** 입금 수동승인(admin) → CONFIRMED + ReservationConfirmed 아웃박스(같은 트랜잭션). */
    @Transactional
    public Reservation confirm(UUID reservationId) {
        Reservation reservation = load(reservationId);
        reservation.confirm();
        reservationRepository.save(reservation);
        drainEventsToOutbox(reservation);
        return reservation;
    }

    /** 취소/만료 → 재고 복원 + CANCELLED + ReservationCancelled 아웃박스(같은 트랜잭션). */
    @Transactional
    public Reservation cancel(UUID reservationId, CancelReason reason) {
        Reservation reservation = load(reservationId);
        reservation.cancel(reason);
        scheduleRepository.restoreStock(reservation.getProductScheduleId(), reservation.getQuantity());
        reservationRepository.save(reservation);
        drainEventsToOutbox(reservation);
        return reservation;
    }

    private Reservation load(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "예약을 찾을 수 없습니다."));
    }

    /** 애그리거트의 도메인 이벤트를 Outbox 행으로 옮긴다(같은 트랜잭션 → 유실 0%). */
    private void drainEventsToOutbox(Reservation reservation) {
        for (DomainEvent event : reservation.domainEvents()) {
            outboxRepository.save(OutboxEvent.ready(
                    AGGREGATE_TYPE, reservation.getId(), event.eventType(), serialize(event)));
        }
        reservation.clearEvents();
    }

    private String serialize(DomainEvent event) {
        try {
            ObjectNode node = objectMapper.valueToTree(event);
            node.put("schemaVersion", event.schemaVersion());
            node.put("occurredAt", OffsetDateTime.now().toString());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 직렬화 실패: " + event.eventType(), e);
        }
    }

    private String generateReservationNo() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("TUT-");
            for (int i = 0; i < 7; i++) {
                sb.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
            }
            String no = sb.toString();
            if (!reservationRepository.existsByReservationNo(no)) {
                return no;
            }
        }
        throw new IllegalStateException("예약번호 생성 실패(충돌 반복)");
    }
}
