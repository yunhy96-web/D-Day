package com.thaiuniontour.domain.catalog.service;

import com.thaiuniontour.domain.catalog.model.ProductSchedule;
import com.thaiuniontour.domain.catalog.model.ScheduleStatus;
import com.thaiuniontour.domain.catalog.port.StockDeductionStrategy;
import com.thaiuniontour.domain.catalog.repository.ProductScheduleRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 기본 재고 차감 전략 — DB 원자 UPDATE (read-modify-write 금지).
 * 동시에 호출돼도 {@code WHERE stock>=qty} 가드로 oversell 0%.
 * 무제한(stock NULL) 일정은 차감 없이 성공 처리. (폭주 특가용 RedisCounterStrategy는 추후 무수정 추가)
 */
@Component
public class AtomicUpdateStockStrategy implements StockDeductionStrategy {

    private final ProductScheduleRepository scheduleRepository;

    public AtomicUpdateStockStrategy(ProductScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public boolean deduct(UUID productScheduleId, int quantity) {
        // 1) 원자 차감 시도. 1행 → 성공.
        if (scheduleRepository.deductStock(productScheduleId, quantity) == 1) {
            return true;
        }
        // 2) 0행 → 무제한 일정이면 성공(차감 불필요), 아니면 매진/마감.
        ProductSchedule s = scheduleRepository.findById(productScheduleId).orElse(null);
        return s != null && s.isUnlimited() && s.getStatus() == ScheduleStatus.OPEN;
    }
}
