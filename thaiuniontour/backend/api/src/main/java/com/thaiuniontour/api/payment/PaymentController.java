package com.thaiuniontour.api.payment;

import com.thaiuniontour.common.response.ApiResponse;
import com.thaiuniontour.domain.payment.model.PaymentHistory;
import com.thaiuniontour.domain.payment.repository.PaymentHistoryRepository;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 결제 상태 조회(고객 폴링). 결제이력(Inbox) 있으면 PAID, 없으면 PENDING. */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentController(PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    @GetMapping("/by-reservation/{reservationId}")
    public ApiResponse<ResPaymentStatus> byReservation(@PathVariable UUID reservationId) {
        return ApiResponse.ok(paymentHistoryRepository.findByReservationId(reservationId)
                .map(ResPaymentStatus::paid)
                .orElseGet(() -> new ResPaymentStatus(reservationId.toString(), "PENDING", null, 0L, null)));
    }

    public record ResPaymentStatus(String reservationId, String status, String method, long amount, String paidAt) {
        static ResPaymentStatus paid(PaymentHistory p) {
            return new ResPaymentStatus(p.getReservationId().toString(), p.getStatus(),
                    p.getMethod(), p.getAmount(), p.getPaidAt() == null ? null : p.getPaidAt().toString());
        }
    }
}
