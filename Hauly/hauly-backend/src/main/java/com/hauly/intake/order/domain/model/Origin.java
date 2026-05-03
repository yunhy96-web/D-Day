package com.hauly.intake.order.domain.model;

/**
 * Where the order originated.
 * ADMIN_INTAKE: registered by an INTAKE user (girlfriend) on behalf of a customer.
 * MARKETPLACE: customer-driven order from the public shop (Phase 2).
 *
 * Mirrors common_code group ORDER_ORIGIN.
 */
public enum Origin {
    ADMIN_INTAKE,
    MARKETPLACE
}
