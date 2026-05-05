package com.hauly.intake.deposit.application.query;

import com.hauly.intake.deposit.domain.model.DepositTransaction;
import com.hauly.intake.deposit.domain.model.DepositTransactionKind;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DepositTransactionView(
        Long id,
        DepositTransactionKind kind,
        BigDecimal amountKrw,
        Long relatedOrderId,
        String relatedOrderNo,
        String note,
        Long createdBy,
        String createdByName,
        OffsetDateTime createdAt
) {
    public static DepositTransactionView from(DepositTransaction tx, String orderNo, String actorName) {
        return new DepositTransactionView(
                tx.getId(), tx.getKind(), tx.getAmountKrw(), tx.getRelatedOrderId(),
                orderNo, tx.getNote(), tx.getCreatedBy(), actorName, tx.getCreatedAt());
    }
}
