package com.flasharena.order.domain;

/**
 * 동시성 시뮬레이션 모드.
 * <ul>
 *   <li>{@link #SYNC} — 락 없음. read-modify-write lost-update 로 oversell 버그를 재현한다.</li>
 *   <li>{@link #REDIS_LOCK} — Redisson 분산 락으로 임계영역을 직렬화하여 정확하지만, 한 명씩 줄세워 느리다.</li>
 *   <li>{@link #REDIS_COUNTER} — Redis 원자 DECR 로 게이트키핑. 락/대기 없이 당첨자만 DB 에 써서
 *       정확하면서도 빠르다(인메모리 원자 연산). 대규모 플래시세일의 현실적 해법.</li>
 * </ul>
 */
public enum SimulationMode {
    SYNC,
    REDIS_LOCK,
    REDIS_COUNTER
}
