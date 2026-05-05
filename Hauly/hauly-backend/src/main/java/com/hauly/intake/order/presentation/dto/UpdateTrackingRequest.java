package com.hauly.intake.order.presentation.dto;

import jakarta.validation.constraints.Size;

/** 한국 택배사 + 송장번호 후속 입력. 둘 다 비어 있으면 트래킹 정보를 비움. */
public record UpdateTrackingRequest(
        @Size(max = 32)  String koreanCourier,
        @Size(max = 500) String koreanTrackingNo
) {}
