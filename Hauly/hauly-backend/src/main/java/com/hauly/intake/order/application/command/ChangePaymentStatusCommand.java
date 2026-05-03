package com.hauly.intake.order.application.command;

import com.hauly.intake.order.domain.model.PaymentStatus;

public record ChangePaymentStatusCommand(
        Long orderId,
        PaymentStatus target,
        String note
) {}
