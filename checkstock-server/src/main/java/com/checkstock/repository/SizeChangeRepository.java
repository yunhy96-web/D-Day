package com.checkstock.repository;

import com.checkstock.entity.SizeChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeChangeRepository extends JpaRepository<SizeChange, String> {
    Page<SizeChange> findByProductIdOrderByDetectedAtDesc(String productId, Pageable pageable);
}
