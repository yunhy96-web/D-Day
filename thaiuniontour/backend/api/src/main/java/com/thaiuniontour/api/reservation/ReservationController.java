package com.thaiuniontour.api.reservation;

import com.thaiuniontour.api.reservation.dto.ReservationDto.BankInfo;
import com.thaiuniontour.api.reservation.dto.ReservationDto.ReqCreate;
import com.thaiuniontour.api.reservation.dto.ReservationDto.ResCreated;
import com.thaiuniontour.api.reservation.dto.ReservationDto.ResDetail;
import com.thaiuniontour.api.reservation.service.ReservationQueryService;
import com.thaiuniontour.common.response.ApiResponse;
import com.thaiuniontour.domain.reservation.model.CancelReason;
import com.thaiuniontour.domain.reservation.model.Reservation;
import com.thaiuniontour.domain.reservation.service.ReservationCommandService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationCommandService commandService;
    private final ReservationQueryService queryService;
    private final String bankName;
    private final String bankAccount;
    private final String bankHolder;

    public ReservationController(ReservationCommandService commandService,
                                 ReservationQueryService queryService,
                                 @Value("${app.bank.name:국민은행}") String bankName,
                                 @Value("${app.bank.account:123456-78-901234}") String bankAccount,
                                 @Value("${app.bank.holder:타이유니온투어}") String bankHolder) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
        this.bankHolder = bankHolder;
    }

    /** 예약 생성(재고차감 + PENDING). 비회원 허용(memberId=null). */
    @PostMapping
    public ApiResponse<ResCreated> create(@Valid @RequestBody ReqCreate req) {
        Reservation r = commandService.create(
                null, req.productScheduleId(), req.quantity(), req.paymentMethod(),
                req.reserver().name(), req.reserver().email(), req.reserver().phone());
        var bank = new BankInfo(bankName, bankAccount, bankHolder,
                r.getPaymentDeadline() == null ? null : r.getPaymentDeadline().toString());
        return ApiResponse.ok(new ResCreated(
                r.getId(), r.getReservationNo(), r.getStatus().name(), r.getAmount(),
                r.getPaymentMethod(), r.getLookupToken().toString(), bank));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResDetail> detail(@PathVariable UUID id) {
        return ApiResponse.ok(queryService.byId(id));
    }

    /** 매직링크 조회(비회원 재진입). */
    @GetMapping("/by-token/{lookupToken}")
    public ApiResponse<ResDetail> byToken(@PathVariable UUID lookupToken) {
        return ApiResponse.ok(queryService.byToken(lookupToken));
    }

    /** 수동 조회(예약번호 + 이메일). */
    @GetMapping("/lookup")
    public ApiResponse<ResDetail> lookup(@RequestParam String no, @RequestParam String email) {
        return ApiResponse.ok(queryService.byLookup(no, email));
    }

    /** 고객 취소 → CANCELLED + 재고복원 + 보상이벤트. */
    @PostMapping("/{id}/cancel")
    public ApiResponse<ResDetail> cancel(@PathVariable UUID id) {
        commandService.cancel(id, CancelReason.CUSTOMER);
        return ApiResponse.ok(queryService.byId(id));
    }
}
