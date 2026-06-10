package com.thaiuniontour.domain.payment.port;

import java.util.UUID;

/**
 * 입금/결제 확인 전략 포트 (확장 seam #2).
 *
 * <p>구현 교체로 무수정 확장:
 * <ul>
 *   <li>MVP {@code ManualConfirmation} — 운영자가 admin에서 입금 수동승인</li>
 *   <li>추후 {@code FirmBankingConfirmation} — 가상계좌/펌뱅킹 웹훅 자동확인(멱등)</li>
 * </ul>
 * 확인 완료 시 결제이력을 멱등 적재한다(payment_history UNIQUE).
 */
public interface PaymentConfirmation {

    /** 이 확인 전략이 처리하는 결제수단 코드. 예: "BANK_TRANSFER". */
    String supportedMethod();

    /** 결제 확인 처리(멱등). 이미 처리된 예약이면 무시. */
    void confirm(UUID reservationId, long amount);
}
