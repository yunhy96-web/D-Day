package com.checkstockbackend.repository;

import com.checkstockbackend.entity.SiteChange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteChangeRepository extends JpaRepository<SiteChange, String> {
}
