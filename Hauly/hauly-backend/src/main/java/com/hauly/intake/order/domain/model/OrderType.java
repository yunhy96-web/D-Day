package com.hauly.intake.order.domain.model;

/**
 * Whether an order is a single product (INDIVIDUAL) or a promotional bundle (SET).
 *
 * SET orders are used for promotions like 2+1 deals where multiple items are bought as
 * one bundle. Constraints enforced at the application layer:
 *  - all items in a SET must share the same categoryId
 *  - SET allows ≥ 1 items (no minimum count enforced; UI guidance only)
 */
public enum OrderType {
    INDIVIDUAL,
    SET
}
