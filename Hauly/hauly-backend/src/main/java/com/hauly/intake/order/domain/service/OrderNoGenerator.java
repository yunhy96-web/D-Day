package com.hauly.intake.order.domain.service;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.time.ZoneId;

/**
 * Generates a human-readable order number from a persisted order id.
 * Format: HL-{yyyy}-{id zero-padded to 4 digits}. Once we exceed 9999/year the
 * suffix grows naturally (HL-2026-12345), still unique.
 *
 * Year is computed in Asia/Seoul because the business operates in KR/TH.
 */
@Component
public class OrderNoGenerator {

    private static final ZoneId BUSINESS_TZ = ZoneId.of("Asia/Seoul");

    public String generate(Long orderId) {
        if (orderId == null || orderId < 1) {
            throw new IllegalArgumentException("orderId must be a positive value");
        }
        int year = Year.now(BUSINESS_TZ).getValue();
        return String.format("HL-%d-%04d", year, orderId);
    }
}
