package com.hauly.intake.order.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Payment lifecycle for an order. Mirrors common_code group PAYMENT_STATUS.
 *
 * NOT_REQUIRED is the default for ADMIN_INTAKE orders during MVP — payment is
 * handled out-of-band (LINE chat / cash). Marketplace orders start as PENDING.
 */
public enum PaymentStatus {
    NOT_REQUIRED,
    PENDING,
    SUBMITTED,
    CONFIRMED,
    REJECTED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(PaymentStatus.class);
        ALLOWED.put(NOT_REQUIRED, EnumSet.of(PENDING));
        ALLOWED.put(PENDING,      EnumSet.of(SUBMITTED, NOT_REQUIRED));
        ALLOWED.put(SUBMITTED,    EnumSet.of(CONFIRMED, REJECTED));
        ALLOWED.put(CONFIRMED,    EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(REJECTED,     EnumSet.of(PENDING, NOT_REQUIRED));
    }

    public boolean canTransitionTo(PaymentStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public Set<PaymentStatus> allowedNext() {
        return EnumSet.copyOf(ALLOWED.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }
}
