package com.hauly.intake.shipping.application.command;

public record CreateShippingTemplateCommand(
        String label,
        String recipientName,
        String recipientPhone,
        String postalCode,
        String addressLine,
        String country
) {}
