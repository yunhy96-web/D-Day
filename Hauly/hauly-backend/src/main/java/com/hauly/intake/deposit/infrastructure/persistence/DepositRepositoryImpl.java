package com.hauly.intake.deposit.infrastructure.persistence;

import com.hauly.intake.deposit.domain.model.DepositTransaction;
import com.hauly.intake.deposit.domain.model.DepositTransactionKind;
import com.hauly.intake.deposit.domain.repository.DepositRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class DepositRepositoryImpl implements DepositRepository {

    private final JpaDepositTransactionRepository jpa;

    DepositRepositoryImpl(JpaDepositTransactionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DepositTransaction save(DepositTransaction tx) {
        return jpa.save(tx);
    }

    @Override
    public BigDecimal currentBalance() {
        BigDecimal sum = jpa.sumAllAmounts();
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public Page<DepositTransaction> listTransactions(Pageable pageable) {
        return jpa.findAll(pageable);
    }

    @Override
    public Optional<DepositTransaction> findPurchaseByOrderId(Long orderId) {
        return jpa.findByOrderAndKind(orderId, DepositTransactionKind.PURCHASE);
    }

    @Override
    public boolean existsRefundForOrderId(Long orderId) {
        return jpa.existsByOrderAndKind(orderId, DepositTransactionKind.REFUND);
    }
}
