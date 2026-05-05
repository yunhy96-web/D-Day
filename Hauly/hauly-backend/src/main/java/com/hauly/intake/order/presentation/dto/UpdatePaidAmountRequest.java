package com.hauly.intake.order.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * PATCH /api/intake/orders/{id}/paid-amount — paidAmountKrw 직접 수정.
 * 차액만큼 디파짓 원장에 ADJUSTMENT 트랜잭션이 자동 기록됨.
 */
public record UpdatePaidAmountRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal paidAmountKrw
) {}
