package com.hauly.intake.order.application.command;

import com.hauly.intake.order.domain.model.FulfillmentStatus;

import java.math.BigDecimal;
import java.util.List;

public record ChangeFulfillmentStatusCommand(
        Long orderId,
        FulfillmentStatus target,
        String note,
        /** Required when target == PURCHASED — debited from the deposit. Null otherwise. */
        BigDecimal paidAmountKrw,
        /** PURCHASED 시 결제 증빙 임시 키 (선택). 영구 키로 이동되어 저장됨. */
        List<String> proofTempKeys
) {}
