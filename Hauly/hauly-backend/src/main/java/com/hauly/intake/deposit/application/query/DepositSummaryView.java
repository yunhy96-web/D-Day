package com.hauly.intake.deposit.application.query;

import java.math.BigDecimal;

/** Summary card data — current balance only for now. */
public record DepositSummaryView(BigDecimal balanceKrw) {}
