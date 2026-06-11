package com.thaiuniontour.domain.catalog.repository;

import com.thaiuniontour.domain.catalog.model.Product;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 상품 리포지토리. 동적 검색/필터는 {@link JpaSpecificationExecutor}로 처리
 * (해당하는 조건만 SQL에 추가 → null 파라미터 타입 문제 회피).
 */
public interface ProductRepository extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {
}
