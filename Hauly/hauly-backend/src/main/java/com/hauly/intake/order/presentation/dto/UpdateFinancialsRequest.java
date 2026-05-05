package com.hauly.intake.order.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * PATCH /api/intake/orders/{id}/financials body.
 * 모든 필드 nullable — 부분 업데이트 가능. amount-currency 쌍은 함께 전송하거나 둘 다 비워야 함.
 */
public record UpdateFinancialsRequest(
        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal customerRevenueAmount,
        @Pattern(regexp = "^(KRW|THB)?$")
        String customerRevenueCurrency,

        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal logisticsKrToThAmount,
        @Pattern(regexp = "^(KRW|THB)?$")
        String logisticsKrToThCurrency,

        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal logisticsThDomesticAmount,
        @Pattern(regexp = "^(KRW|THB)?$")
        String logisticsThDomesticCurrency,

        @DecimalMin(value = "0.0001")
        @Digits(integer = 6, fraction = 4)
        BigDecimal krwPerThb
) {}
