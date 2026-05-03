package com.hauly.intake.order.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateOrderRequest(
        @NotBlank @Size(max = 64) String customerName,
        @Size(max = 64)            String customerLineId,
        @Size(max = 32)            String customerPhone,
                                   String customerMemo,
                                   String internalMemo,
        @Size(max = 64)            String koreanTrackingNo,
        @Size(max = 32)            String koreanCourier,
        @NotEmpty @Valid           List<Item> items
) {
    public record Item(
            @NotBlank @Size(max = 255) String productName,
                                       String productUrl,
            @NotNull @Min(1)           Integer quantity,
                                       Long categoryId,
                                       Map<String, Object> attributes,
            @DecimalMin(value = "0.00")
            @Digits(integer = 10, fraction = 2)
                                       BigDecimal unitPriceAmount,
            @Pattern(regexp = "^(KRW|THB|USD)?$")
                                       String unitPriceCurrency,
            @Size(max = 5)             List<String> tempImageKeys
    ) {}
}
