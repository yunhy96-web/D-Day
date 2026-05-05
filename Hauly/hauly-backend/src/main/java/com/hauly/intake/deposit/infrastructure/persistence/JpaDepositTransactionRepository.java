package com.hauly.intake.deposit.infrastructure.persistence;

import com.hauly.intake.deposit.domain.model.DepositTransaction;
import com.hauly.intake.deposit.domain.model.DepositTransactionKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
interface JpaDepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {

    @Query("SELECT COALESCE(SUM(t.amountKrw), 0) FROM DepositTransaction t")
    BigDecimal sumAllAmounts();

    @Query("SELECT t FROM DepositTransaction t WHERE t.relatedOrderId = :orderId AND t.kind = :kind")
    Optional<DepositTransaction> findByOrderAndKind(@Param("orderId") Long orderId,
                                                    @Param("kind") DepositTransactionKind kind);

    @Query("SELECT (COUNT(t) > 0) FROM DepositTransaction t WHERE t.relatedOrderId = :orderId AND t.kind = :kind")
    boolean existsByOrderAndKind(@Param("orderId") Long orderId,
                                 @Param("kind") DepositTransactionKind kind);
}
