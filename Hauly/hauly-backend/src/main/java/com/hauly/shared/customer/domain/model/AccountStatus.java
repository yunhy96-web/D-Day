package com.hauly.shared.customer.domain.model;

/**
 * Customer account lifecycle.
 * GUEST: created on the fly during intake (no signup).
 * REGISTERED: signed up via marketplace (Phase 2).
 */
public enum AccountStatus {
    GUEST,
    REGISTERED
}
