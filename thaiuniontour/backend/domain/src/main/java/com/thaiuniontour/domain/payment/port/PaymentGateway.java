package com.thaiuniontour.domain.payment.port;

/**
 * 결제 게이트웨이 포트 (확장 seam #1). 결제수단별 처리 추상화.
 *
 * <p>신규 결제수단 추가 = 공통코드(PAYMENT_METHOD) 1건 + 이 인터페이스 구현 1개(기존 무수정).
 * <ul>
 *   <li>현재: {@code BankTransferGateway} (계좌이체, 수동)</li>
 *   <li>추후: NaverPay/KakaoPay/TossPay/Card Gateway (prepare/confirm/webhook — M4)</li>
 * </ul>
 * 구현체는 {@link #supportedMethod()}로 자신이 처리하는 코드를 알리고,
 * 디스패처가 결제수단 코드로 적절한 구현을 선택한다. (메서드 시그니처는 M2에서 확정)
 */
public interface PaymentGateway {

    /** 이 게이트웨이가 처리하는 결제수단 코드(common_code PAYMENT_METHOD). 예: "BANK_TRANSFER". */
    String supportedMethod();
}
