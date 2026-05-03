package com.hauly.intake.order.domain.model;

/**
 * Two independent status axes on an order.
 * Logged separately in order_status_log so the audit trail records which dimension changed.
 */
public enum StatusDimension {
    FULFILLMENT,
    PAYMENT
}
