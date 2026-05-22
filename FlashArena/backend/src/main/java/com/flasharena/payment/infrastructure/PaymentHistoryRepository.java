package com.flasharena.payment.infrastructure;

import com.flasharena.payment.domain.PaymentHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

    /** 멱등성 빠른 경로: 이미 처리된 주문인지 선체크 (최종 방어선은 DB UNIQUE). */
    boolean existsByOrderId(UUID orderId);

    /** 시뮬레이터 리셋: 결제 이력 잔여물 삭제 (재현 가능성 확보). */
    @Transactional
    @Modifying
    @Query("DELETE FROM PaymentHistory p")
    void deleteAllHistory();
}
