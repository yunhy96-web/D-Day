package com.hauly.intake.order.application.command;

import com.hauly.intake.order.domain.model.PaymentStatus;

public record ForcePaymentStatusCommand(
        Long orderId,
        PaymentStatus target,
        String reason
) {}
