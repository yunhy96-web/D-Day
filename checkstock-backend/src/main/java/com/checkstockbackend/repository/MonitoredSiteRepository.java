package com.checkstockbackend.repository;

import com.checkstockbackend.entity.MonitoredSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitoredSiteRepository extends JpaRepository<MonitoredSite, String> {
    List<MonitoredSite> findByActiveTrue();
}
