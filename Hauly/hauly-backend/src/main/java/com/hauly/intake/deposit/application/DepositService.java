package com.hauly.intake.deposit.application;

import com.hauly.intake.deposit.application.command.RecordAdjustmentCommand;
import com.hauly.intake.deposit.application.query.DepositSummaryView;
import com.hauly.intake.deposit.application.query.DepositTransactionView;
import com.hauly.intake.deposit.domain.model.DepositTransaction;
import com.hauly.intake.deposit.domain.repository.DepositRepository;
import com.hauly.intake.order.domain.repository.OrderRepository;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deposit ledger orchestration. Single global KRW balance kept as the SUM of
 * {@link DepositTransaction} amounts (no separate balance row).
 *
 * <p>Order integration is invoked from {@code IntakeOrderService}: PURCHASED transitions
 * call {@link #recordPurchase}, and CANCELLED transitions call {@link #refundIfPurchased}.
 */
@Service
@Transactional
public class DepositService {

    private final DepositRepository deposits;
    private final OrderRepository orders;
    private final AppUserRepository users;

    public DepositService(DepositRepository deposits, OrderRepository orders, AppUserRepository users) {
        this.deposits = deposits;
        this.orders = orders;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public DepositSummaryView summary() {
        return new DepositSummaryView(deposits.currentBalance());
    }

    @Transactional(readOnly = true)
    public Page<DepositTransactionView> listTransactions(int page, int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DepositTransaction> txs = deposits.listTransactions(pageable);

        Set<Long> orderIds = txs.stream()
                .map(DepositTransaction::getRelatedOrderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> orderNos = orders.findOrderNosByIds(orderIds);

        Set<Long> userIds = txs.stream()
                .map(DepositTransaction::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> userNames = users.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        AppUser::getId,
                        u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsernameValue()));

        return txs.map(tx -> DepositTransactionView.from(
                tx,
                tx.getRelatedOrderId() == null ? null : orderNos.get(tx.getRelatedOrderId()),
                tx.getCreatedBy() == null ? null : userNames.get(tx.getCreatedBy())));
    }

    public DepositTransactionView recordAdjustment(RecordAdjustmentCommand cmd, Long actorId) {
        if (cmd.amountKrw() == null || cmd.amountKrw().signum() == 0) {
            throw new IllegalArgumentException("deposit_amount_invalid");
        }
        DepositTransaction tx = deposits.save(
                DepositTransaction.adjustment(cmd.amountKrw(), trimToNull(cmd.note()), actorId));
        return toView(tx);
    }

    /**
     * Order PURCHASED hook — debits the deposit by {@code paidAmount}. No-op if {@code paidAmount}
     * is null/zero (callers must validate beforehand for PURCHASED transitions).
     */
    public void recordPurchase(Long orderId, BigDecimal paidAmount, Long actorId) {
        if (paidAmount == null || paidAmount.signum() <= 0) {
            throw new IllegalArgumentException("paid_amount_required");
        }
        deposits.save(DepositTransaction.purchase(orderId, paidAmount, actorId));
    }

    /**
     * paidAmountKrw 수정 보정 — 기존 PURCHASE 트랜잭션은 그대로 두고 차액만큼 ADJUSTMENT 추가.
     * Append-only 원장 유지. delta = newPaid - oldPaid; ADJUSTMENT amount = -delta (PURCHASE는 음수이므로).
     */
    public void recordPaidAmountAdjustment(Long orderId, BigDecimal oldPaid, BigDecimal newPaid,
                                           String note, Long actorId) {
        if (oldPaid == null || newPaid == null) {
            throw new IllegalArgumentException("paid_amount_required");
        }
        BigDecimal delta = newPaid.subtract(oldPaid);
        if (delta.signum() == 0) return; // no change
        BigDecimal adjustmentAmount = delta.negate(); // PURCHASE was -X, so increase debit means another negative.
        deposits.save(DepositTransaction.adjustmentForOrder(
                orderId, adjustmentAmount, trimToNull(note), actorId));
    }

    /**
     * Order CANCELLED hook — credits back the original PURCHASE amount, if any. Idempotent:
     * skipped when no PURCHASE was recorded for this order, or when a REFUND is already present.
     */
    public void refundIfPurchased(Long orderId, Long actorId, String note) {
        if (deposits.existsRefundForOrderId(orderId)) return;
        deposits.findPurchaseByOrderId(orderId).ifPresent(purchase ->
                deposits.save(DepositTransaction.refund(
                        orderId, purchase.getAmountKrw().negate(), trimToNull(note), actorId)));
    }

    private DepositTransactionView toView(DepositTransaction tx) {
        String orderNo = tx.getRelatedOrderId() == null
                ? null
                : orders.findOrderNosByIds(java.util.List.of(tx.getRelatedOrderId()))
                        .get(tx.getRelatedOrderId());
        String actorName = tx.getCreatedBy() == null
                ? null
                : users.findById(tx.getCreatedBy())
                        .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsernameValue())
                        .orElse(null);
        return DepositTransactionView.from(tx, orderNo, actorName);
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
