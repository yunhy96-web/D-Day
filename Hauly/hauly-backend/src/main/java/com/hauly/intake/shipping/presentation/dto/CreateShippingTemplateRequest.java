package com.hauly.intake.shipping.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShippingTemplateRequest(
        @NotBlank @Size(max = 64)  String label,
        @Size(max = 64)            String recipientName,
        @Size(max = 32)            String recipientPhone,
        @Size(max = 16)            String postalCode,
        @Size(max = 1000)          String addressLine,
        @Pattern(regexp = "^[A-Za-z]{0,2}$")
                                   String country
) {}
