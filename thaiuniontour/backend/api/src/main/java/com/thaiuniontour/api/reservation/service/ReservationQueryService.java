package com.thaiuniontour.api.reservation.service;

import com.thaiuniontour.api.reservation.dto.ReservationDto.ResDetail;
import com.thaiuniontour.common.error.ErrorCode;
import com.thaiuniontour.common.exception.BusinessException;
import com.thaiuniontour.domain.reservation.model.Reservation;
import com.thaiuniontour.domain.reservation.repository.ReservationRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 예약 조회(상세/매직링크/수동조회). */
@Service
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;

    public ReservationQueryService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public ResDetail byId(UUID id) {
        return toDetail(reservationRepository.findById(id)
                .orElseThrow(() -> notFound()));
    }

    @Transactional(readOnly = true)
    public ResDetail byToken(UUID lookupToken) {
        return toDetail(reservationRepository.findByLookupToken(lookupToken)
                .orElseThrow(() -> notFound()));
    }

    @Transactional(readOnly = true)
    public ResDetail byLookup(String reservationNo, String email) {
        return toDetail(reservationRepository.findByReservationNoAndReserverEmail(reservationNo, email)
                .orElseThrow(() -> notFound()));
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.COMMON_NOT_FOUND, "예약을 찾을 수 없습니다.");
    }

    private ResDetail toDetail(Reservation r) {
        return new ResDetail(
                r.getId(), r.getReservationNo(), r.getStatus().name(),
                r.getProductId(), r.getProductScheduleId(), str(r.getSaleDate()),
                r.getQuantity(), r.getAmount(), r.getPaymentMethod(),
                new ResDetail.ReserverInfo(r.getReserverName(), r.getReserverEmail(), r.getReserverPhone()),
                str(r.getPaymentDeadline()), str(r.getConfirmedAt()), str(r.getCancelledAt()));
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private String str(OffsetDateTime o) {
        return o == null ? null : o.toString();
    }
}
