package com.hauly.intake.shipping.application.query;

import com.hauly.intake.shipping.domain.model.ShippingAddressTemplate;

public record ShippingAddressTemplateView(
        Long id,
        String label,
        String recipientName,
        String recipientPhone,
        String postalCode,
        String addressLine,
        String country
) {
    public static ShippingAddressTemplateView from(ShippingAddressTemplate t) {
        return new ShippingAddressTemplateView(
                t.getId(), t.getLabel(),
                t.getRecipientName(), t.getRecipientPhone(),
                t.getPostalCode(), t.getAddressLine(), t.getCountry());
    }
}
