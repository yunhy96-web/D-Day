package com.hauly.intake.order.application.command;

import com.hauly.intake.order.domain.model.FulfillmentStatus;

public record ForceFulfillmentStatusCommand(
        Long orderId,
        FulfillmentStatus target,
        String reason
) {}
