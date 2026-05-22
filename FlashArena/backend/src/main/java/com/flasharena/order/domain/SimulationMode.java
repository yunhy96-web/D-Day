package com.flasharena.order.domain;

/**
 * 동시성 시뮬레이션 모드.
 * <ul>
 *   <li>{@link #SYNC} — 락 없음. read-modify-write lost-update 로 oversell 버그를 재현한다.</li>
 *   <li>{@link #REDIS_LOCK} — Redisson 분산 락으로 직렬화하여 정확한 결과를 보장한다.</li>
 * </ul>
 */
public enum SimulationMode {
    SYNC,
    REDIS_LOCK
}
