package com.flasharena.order.presentation.dto;

import com.flasharena.order.domain.SimulationMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 시뮬레이션 실행 요청.
 * concurrency = 동시 요청 수(각 요청은 상품 1개 구매 시도). RAM-1GB 보호를 위해 1..20000 으로 제한.
 */
public record SimulationRequest(
        @NotNull(message = "mode 는 필수입니다. (SYNC | REDIS_LOCK | REDIS_COUNTER)")
        SimulationMode mode,

        @Min(value = 1, message = "concurrency 는 1 이상이어야 합니다.")
        @Max(value = 20000, message = "concurrency 는 20000 이하여야 합니다.")
        Integer concurrency,

        @Min(value = 0, message = "initialStock 은 0 이상이어야 합니다.")
        Integer initialStock) {

    private static final int DEFAULT_CONCURRENCY = 1000;
    private static final int DEFAULT_INITIAL_STOCK = 100;

    /** 미지정 시 기본값(동시 요청 1000, 초기 재고 100) 적용. */
    public int concurrencyOrDefault() {
        return concurrency != null ? concurrency : DEFAULT_CONCURRENCY;
    }

    public int initialStockOrDefault() {
        return initialStock != null ? initialStock : DEFAULT_INITIAL_STOCK;
    }
}
