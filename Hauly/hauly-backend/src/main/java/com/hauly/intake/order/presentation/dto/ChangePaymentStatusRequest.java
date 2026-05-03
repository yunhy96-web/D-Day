package com.hauly.intake.order.presentation.dto;

import com.hauly.intake.order.domain.model.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangePaymentStatusRequest(
        @NotNull PaymentStatus target,
                 String note
) {}
