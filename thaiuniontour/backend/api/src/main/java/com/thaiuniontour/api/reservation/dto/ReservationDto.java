package com.thaiuniontour.api.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** 예약 API DTO 모음. */
public final class ReservationDto {

    private ReservationDto() {}

    /** 예약 생성 요청. 비회원은 reserver 정보 필수. */
    public record ReqCreate(
            @NotNull UUID productScheduleId,
            @Min(1) int quantity,
            @NotBlank String paymentMethod,
            @NotNull @Valid Reserver reserver
    ) {
        public record Reserver(
                @NotBlank String name,
                @NotBlank @Email String email,
                @NotBlank String phone
        ) {}
    }

    /** 계좌이체 안내(공용). PENDING 입금 대기 시 노출. */
    public record BankInfo(String bank, String account, String holder, String deadline) {}

    /** 예약 생성 응답: 계좌이체 안내 + 매직링크 토큰 포함. */
    public record ResCreated(
            UUID reservationId,
            String reservationNo,
            String status,
            long amount,
            String paymentMethod,
            String lookupToken,
            BankInfo bankInfo
    ) {}

    /** 예약 상세/조회 응답. PENDING이면 bankInfo 포함. */
    public record ResDetail(
            UUID id,
            String reservationNo,
            String status,
            UUID productId,
            UUID productScheduleId,
            String saleDate,
            int quantity,
            long amount,
            String paymentMethod,
            ReserverInfo reserver,
            String paymentDeadline,
            String confirmedAt,
            String cancelledAt,
            BankInfo bankInfo
    ) {
        public record ReserverInfo(String name, String email, String phone) {}
    }
}
