package com.thaiuniontour.domain.catalog.port;

import java.util.UUID;

/**
 * 재고 차감 전략 포트 (확장 seam #3). 선착순 동시성의 핵심.
 *
 * <p>구현 교체로 무수정 확장(reference-patterns.md):
 * <ul>
 *   <li>기본 {@code AtomicUpdateStrategy} — {@code UPDATE ... SET stock=stock-:qty WHERE stock>=:qty}</li>
 *   <li>폭주 특가 {@code RedisCounterStrategy} — Redis DECR 게이트 + DB 원자 차감</li>
 * </ul>
 * read-modify-write 금지 원칙을 구현 뒤로 숨긴다. (M2에서 구현)
 */
public interface StockDeductionStrategy {

    /**
     * 재고를 원자적으로 차감 시도한다.
     *
     * @return 차감 성공(재고 확보) 여부. false = 매진(예약 실패 처리).
     */
    boolean deduct(UUID productScheduleId, int quantity);
}
