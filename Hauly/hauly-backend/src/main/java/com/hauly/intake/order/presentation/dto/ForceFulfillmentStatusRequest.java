package com.hauly.intake.order.presentation.dto;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ForceFulfillmentStatusRequest(
        @NotNull FulfillmentStatus target,
        @NotBlank @Size(max = 1000) String reason
) {}
