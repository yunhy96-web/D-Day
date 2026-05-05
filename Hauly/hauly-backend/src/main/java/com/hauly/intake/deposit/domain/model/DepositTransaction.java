package com.hauly.intake.deposit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Append-only ledger entry for the global KRW deposit. Balance is computed as
 * {@code SUM(amount_krw)} across all rows — no separate balance cache.
 */
@Entity
@Table(name = "deposit_transaction")
public class DepositTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DepositTransactionKind kind;

    /** Signed: positive = credit (TOP_UP/REFUND/positive ADJUSTMENT), negative = debit. */
    @Column(name = "amount_krw", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountKrw;

    /** Set for PURCHASE / REFUND; null for TOP_UP / ADJUSTMENT. */
    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** JPA only. */
    protected DepositTransaction() {}

    private DepositTransaction(DepositTransactionKind kind, BigDecimal amountKrw,
                               Long relatedOrderId, String note, Long createdBy) {
        this.kind = kind;
        this.amountKrw = amountKrw;
        this.relatedOrderId = relatedOrderId;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
    }

    public static DepositTransaction adjustment(BigDecimal amountKrw, String note, Long createdBy) {
        if (amountKrw == null || amountKrw.signum() == 0) {
            throw new IllegalArgumentException("deposit_amount_invalid");
        }
        return new DepositTransaction(DepositTransactionKind.ADJUSTMENT, amountKrw, null, note, createdBy);
    }

    /** 주문에 연결된 ADJUSTMENT — paidAmountKrw 보정 등에 사용. */
    public static DepositTransaction adjustmentForOrder(Long orderId, BigDecimal amountKrw,
                                                        String note, Long createdBy) {
        if (amountKrw == null || amountKrw.signum() == 0) {
            throw new IllegalArgumentException("deposit_amount_invalid");
        }
        return new DepositTransaction(DepositTransactionKind.ADJUSTMENT, amountKrw, orderId, note, createdBy);
    }

    public static DepositTransaction topUp(BigDecimal amountKrw, String note, Long createdBy) {
        if (amountKrw == null || amountKrw.signum() <= 0) {
            throw new IllegalArgumentException("deposit_amount_invalid");
        }
        return new DepositTransaction(DepositTransactionKind.TOP_UP, amountKrw, null, note, createdBy);
    }

    /** Records a debit for an order purchase. {@code paidAmount} must be positive; stored as negative. */
    public static DepositTransaction purchase(Long orderId, BigDecimal paidAmount, Long createdBy) {
        if (paidAmount == null || paidAmount.signum() <= 0) {
            throw new IllegalArgumentException("deposit_amount_invalid");
        }
        return new DepositTransaction(
                DepositTransactionKind.PURCHASE, paidAmount.negate(), orderId, null, createdBy);
    }

    /** Records a credit reversal for a cancelled order. {@code amount} must be positive. */
    public static DepositTransaction refund(Long orderId, BigDecimal amount, String note, Long createdBy) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("deposit_amount_invalid");
        }
        return new DepositTransaction(
                DepositTransactionKind.REFUND, amount, orderId, note, createdBy);
    }

    public Long getId() { return id; }
    public DepositTransactionKind getKind() { return kind; }
    public BigDecimal getAmountKrw() { return amountKrw; }
    public Long getRelatedOrderId() { return relatedOrderId; }
    public String getNote() { return note; }
    public Long getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
