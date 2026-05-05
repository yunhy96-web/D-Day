package com.hauly.intake.order.presentation.dto;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ChangeFulfillmentStatusRequest(
        @NotNull FulfillmentStatus target,
                 String note,
        /** KRW amount actually paid — required when target == PURCHASED. */
        BigDecimal paidAmountKrw,
        /** PURCHASED 시 결제 증빙 임시 이미지 키. 비어 있어도 됨. */
        @Size(max = 5) List<String> proofTempKeys
) {}
