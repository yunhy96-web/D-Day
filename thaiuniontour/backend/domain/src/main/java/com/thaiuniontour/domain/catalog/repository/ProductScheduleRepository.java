package com.thaiuniontour.domain.catalog.repository;

import com.thaiuniontour.domain.catalog.model.ProductSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductScheduleRepository extends JpaRepository<ProductSchedule, UUID> {

    List<ProductSchedule> findByProductIdOrderBySaleDateAsc(UUID productId);

    List<ProductSchedule> findByProductIdAndSaleDateBetweenOrderBySaleDateAsc(
            UUID productId, LocalDate from, LocalDate to);

    /**
     * 재고 원자 차감(선착순 핵심). 영향 행 1 → 성공, 0 → 재고부족.
     * 무제한(stock IS NULL)은 여기서 0을 반환하므로 호출부가 별도 판단.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductSchedule s SET s.stock = s.stock - :qty
         WHERE s.id = :id AND s.stock IS NOT NULL AND s.stock >= :qty
        """)
    int deductStock(@Param("id") UUID id, @Param("qty") int qty);

    /** 재고 복원(취소/만료 보상). 무제한은 영향 없음. */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductSchedule s SET s.stock = s.stock + :qty
         WHERE s.id = :id AND s.stock IS NOT NULL
        """)
    int restoreStock(@Param("id") UUID id, @Param("qty") int qty);
}
