package com.hauly.intake.order.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Fulfillment lifecycle for an order. Mirrors common_code group FULFILLMENT_STATUS.
 *
 * Allowed transitions are encoded here so {@link Order} can guard state changes.
 * Terminal states (COMPLETED / CANCELLED / REJECTED) have no outgoing edges.
 */
public enum FulfillmentStatus {
    DRAFT,
    REQUESTED,
    ACKNOWLEDGED,
    PURCHASING,
    PURCHASED,
    SHIPPED_TO_AGENT,
    COMPLETED,
    OUT_OF_STOCK,
    CANCELLED,
    REJECTED;

    private static final Map<FulfillmentStatus, Set<FulfillmentStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(FulfillmentStatus.class);
        ALLOWED.put(DRAFT,            EnumSet.of(REQUESTED, CANCELLED));
        ALLOWED.put(REQUESTED,        EnumSet.of(ACKNOWLEDGED, REJECTED, CANCELLED));
        ALLOWED.put(ACKNOWLEDGED,     EnumSet.of(PURCHASING, CANCELLED));
        ALLOWED.put(PURCHASING,       EnumSet.of(PURCHASED, OUT_OF_STOCK, CANCELLED));
        ALLOWED.put(PURCHASED,        EnumSet.of(SHIPPED_TO_AGENT, CANCELLED));
        ALLOWED.put(SHIPPED_TO_AGENT, EnumSet.of(COMPLETED));
        ALLOWED.put(OUT_OF_STOCK,     EnumSet.of(PURCHASING, CANCELLED));
        ALLOWED.put(COMPLETED,        EnumSet.noneOf(FulfillmentStatus.class));
        ALLOWED.put(CANCELLED,        EnumSet.noneOf(FulfillmentStatus.class));
        ALLOWED.put(REJECTED,         EnumSet.noneOf(FulfillmentStatus.class));
    }

    public boolean canTransitionTo(FulfillmentStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public Set<FulfillmentStatus> allowedNext() {
        return EnumSet.copyOf(ALLOWED.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }
}
