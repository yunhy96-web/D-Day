package com.hauly.platform.commoncode.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCodeRequest(
        @NotBlank String code,
        @NotBlank String nameKo,
        String nameEn,
        String nameTh,
        int sortOrder
) {}
