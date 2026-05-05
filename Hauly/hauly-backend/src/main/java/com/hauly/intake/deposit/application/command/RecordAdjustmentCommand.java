package com.hauly.intake.deposit.application.command;

import java.math.BigDecimal;

public record RecordAdjustmentCommand(
        BigDecimal amountKrw,
        String note
) {}
