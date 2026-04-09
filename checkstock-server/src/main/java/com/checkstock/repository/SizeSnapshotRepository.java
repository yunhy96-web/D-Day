package com.checkstock.repository;

import com.checkstock.entity.SizeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SizeSnapshotRepository extends JpaRepository<SizeSnapshot, String> {
    Optional<SizeSnapshot> findTopByProductIdOrderByCheckedAtDesc(String productId);
}
