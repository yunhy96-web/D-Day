package com.hauly.intake.deposit.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordAdjustmentRequest(
        @NotNull BigDecimal amountKrw,
        @Size(max = 1000) String note
) {}
