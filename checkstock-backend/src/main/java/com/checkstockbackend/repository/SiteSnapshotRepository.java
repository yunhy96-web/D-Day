package com.checkstockbackend.repository;

import com.checkstockbackend.entity.SiteSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteSnapshotRepository extends JpaRepository<SiteSnapshot, String> {
    Optional<SiteSnapshot> findTopBySiteIdOrderByCheckedAtDesc(String siteId);
}
