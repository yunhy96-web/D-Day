package com.hauly.intake.deposit.domain.repository;

import com.hauly.intake.deposit.domain.model.DepositTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

/** Domain repository for the deposit ledger. Single global balance. */
public interface DepositRepository {

    DepositTransaction save(DepositTransaction tx);

    /** Sum of all amounts. Returns 0 when the ledger is empty. */
    BigDecimal currentBalance();

    Page<DepositTransaction> listTransactions(Pageable pageable);

    /** The PURCHASE entry recorded for a given order, if any. */
    Optional<DepositTransaction> findPurchaseByOrderId(Long orderId);

    /** True when a REFUND has already been written for this order — prevents double-refund. */
    boolean existsRefundForOrderId(Long orderId);
}
