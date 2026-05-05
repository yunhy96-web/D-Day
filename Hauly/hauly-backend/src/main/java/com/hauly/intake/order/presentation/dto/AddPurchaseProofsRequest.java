package com.hauly.intake.order.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddPurchaseProofsRequest(
        @NotEmpty @Size(max = 5) List<String> proofTempKeys
) {}
