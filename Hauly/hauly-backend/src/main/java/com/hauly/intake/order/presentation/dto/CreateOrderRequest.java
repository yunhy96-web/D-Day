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
        @NotBlank @Size(max = 64)  String customerName,
        @Size(max = 500)           String customerLineId,
        @Size(max = 32)            String customerPhone,
        @Pattern(regexp = "^(INDIVIDUAL|SET)?$")
                                   String orderType,
                                   String customerMemo,
                                   String internalMemo,
        @Size(max = 500)           String koreanTrackingNo,
        @Size(max = 32)            String koreanCourier,
        // Shipping address (all optional)
        @Size(max = 64)            String recipientName,
        @Size(max = 32)            String recipientPhone,
        @Size(max = 16)            String postalCode,
        @Size(max = 1000)          String addressLine,
        @Pattern(regexp = "^[A-Za-z]{0,2}$")
                                   String country,
        @Size(max = 64)            String shippingAddressLabel,
        @NotEmpty @Valid           List<Item> items
) {
    public record Item(
            @NotBlank @Size(max = 255) String productName,
                                       String productUrl,
            @NotNull @Min(1)           Integer quantity,
                                       Long categoryId,
                                       Map<String, Object> attributes,
            // 등록 시 단가/통화 모두 필수 — 디파짓/매출 계산을 위해 추후 채우는 게 아니라 처음부터 보장.
            // 0원 주문은 무의미하므로 0.01 이상.
            @NotNull
            @DecimalMin(value = "0.01")
            @Digits(integer = 10, fraction = 2)
                                       BigDecimal unitPriceAmount,
            @NotBlank
            @Pattern(regexp = "^(KRW|THB|USD)$")
                                       String unitPriceCurrency,
            @Size(max = 5)             List<String> tempImageKeys
    ) {}
}
