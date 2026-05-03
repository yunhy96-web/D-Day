package com.hauly.shared.customer.application.command;

/**
 * Command: identify (or create) a customer by name + at least one of (lineId, phone).
 * INTAKE flow always provides a name; lineId/phone are optional but at least one is recommended
 * so the customer can be matched on subsequent orders.
 */
public record IdentifyCustomerCommand(
        String name,
        String lineId,
        String phone
) {}
