package com.flasharena.order.presentation.dto;

import com.flasharena.order.domain.SimulationMode;
import java.time.OffsetDateTime;

/**
 * 시뮬레이션 결과 리포트.
 * expectedStock = max(0, initialStock - successCount) 가 올바른 값이며,
 * oversold 는 successCount 초과 / 음수 재고 / 재고 불일치 중 하나라도 발생하면 true.
 * runId 는 Phase 5 SSE 구독 키로 쓰인다.
 */
public record SimulationResult(
        String runId,
        SimulationMode mode,
        int concurrency,
        int initialStock,
        int successCount,
        int failCount,
        int finalStock,
        int expectedStock,
        boolean oversold,
        long elapsedMs,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt) {
}
