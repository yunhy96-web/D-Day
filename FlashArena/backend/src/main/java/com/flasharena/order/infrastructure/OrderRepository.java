package com.flasharena.order.infrastructure;

import com.flasharena.order.domain.OrderEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /** 매 run 시작 전 해당 상품의 주문 내역을 지워 재현 가능성을 확보한다. */
    @Transactional
    void deleteByProductId(UUID productId);

    /** 특정 상품/상태(CREATED·FAILED)의 주문 수를 센다. */
    long countByProductIdAndStatus(UUID productId, String status);
}
