package com.hauly.intake.deposit.domain.model;

/**
 * Deposit ledger transaction types.
 *
 * <ul>
 *   <li>{@link #TOP_UP} — customer added KRW to the deposit (positive amount).</li>
 *   <li>{@link #PURCHASE} — auto-debit when an order transitions to PURCHASED (negative amount).</li>
 *   <li>{@link #REFUND} — auto-credit when a previously-purchased order is CANCELLED (positive).</li>
 *   <li>{@link #ADJUSTMENT} — manual ADMIN correction (signed). Used for the initial balance and
 *       any later reconciliation.</li>
 * </ul>
 */
public enum DepositTransactionKind {
    TOP_UP,
    PURCHASE,
    REFUND,
    ADJUSTMENT
}
