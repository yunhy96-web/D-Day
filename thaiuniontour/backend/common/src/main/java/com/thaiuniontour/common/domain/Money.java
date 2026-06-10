package com.thaiuniontour.common.domain;

/**
 * 금액 값 객체(VO) — KRW 단일 통화, 원 단위(long). 음수 금지.
 * 02-domain.md의 Money(VO)에 해당. 예약·결제 등에서 공유.
 */
public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다: " + amount);
        }
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public static final Money ZERO = new Money(0);

    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money times(int qty) {
        return new Money(this.amount * qty);
    }

    public static final String CURRENCY = "KRW";
}
