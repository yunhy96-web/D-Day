package com.hauly.intake.order.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderNoteRequest(
        @NotBlank @Size(max = 2000) String body
) {}
