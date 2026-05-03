/**
 * Intake bounded context — order registration and lifecycle management for the
 * INTAKE user (girlfriend) and BUYER user (purchaser).
 *
 * Aggregate root: Order. Children: OrderItem, OrderStatusLog.
 * Two independent status dimensions (fulfillment + payment) governed by enum-encoded
 * transition rules — see FulfillmentStatus and PaymentStatus.
 */
package com.hauly.intake.order;
