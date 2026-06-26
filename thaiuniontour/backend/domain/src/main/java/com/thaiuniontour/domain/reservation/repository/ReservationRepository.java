package com.thaiuniontour.domain.reservation.repository;

import com.thaiuniontour.domain.reservation.model.Reservation;
import com.thaiuniontour.domain.reservation.model.ReservationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByReservationNo(String reservationNo);

    /** 매직링크 조회(비회원 재진입). */
    Optional<Reservation> findByLookupToken(UUID lookupToken);

    /** 수동 조회(예약번호 + 이메일). */
    Optional<Reservation> findByReservationNoAndReserverEmail(String reservationNo, String reserverEmail);

    List<Reservation> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    /** 입금기한 만료 스위퍼: PENDING + 기한초과. */
    List<Reservation> findByStatusAndPaymentDeadlineBefore(ReservationStatus status, OffsetDateTime now);

    /** admin 전체 목록(최신순). */
    List<Reservation> findAllByOrderByCreatedAtDesc();

    /** admin 상태별 목록(최신순). */
    List<Reservation> findByStatusOrderByCreatedAtDesc(ReservationStatus status);
}
