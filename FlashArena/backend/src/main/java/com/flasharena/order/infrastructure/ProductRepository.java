package com.flasharena.order.infrastructure;

import com.flasharena.order.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /** 시뮬레이터가 사용하는 단일 시드 상품을 가장 먼저 생성된 순으로 조회한다. */
    Optional<Product> findFirstByOrderByCreatedAtAsc();
}
