package com.hauly.intake.order.presentation.dto;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeFulfillmentStatusRequest(
        @NotNull FulfillmentStatus target,
                 String note
) {}
