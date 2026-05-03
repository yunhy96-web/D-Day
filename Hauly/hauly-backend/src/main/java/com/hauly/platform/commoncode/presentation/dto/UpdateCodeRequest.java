package com.hauly.platform.commoncode.presentation.dto;

public record UpdateCodeRequest(
        String nameKo,
        String nameEn,
        String nameTh,
        Integer sortOrder,
        Boolean active
) {}
