package com.flasharena.order.infrastructure;

import com.flasharena.order.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /** 시뮬레이터가 사용하는 단일 시드 상품을 가장 먼저 생성된 순으로 조회한다. */
    Optional<Product> findFirstByOrderByCreatedAtAsc();

    /**
     * 재고 1 원자 차감 (read-modify-write 가 아니라 DB 단일 UPDATE).
     * REDIS_COUNTER 모드에서 게이트(DECR)를 통과한 당첨자들이 동시에 호출해도
     * lost-update 없이 quantity 가 정확히 줄어든다. 게이트가 음수 차감을 막으므로 WHERE 가드는 불필요.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.quantity = p.quantity - 1 WHERE p.id = :id")
    void decreaseQuantityAtomic(@Param("id") UUID id);
}
