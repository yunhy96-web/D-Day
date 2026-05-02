package com.checkstockbackend.repository;

import com.checkstockbackend.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {
    Optional<DeviceToken> findByExpoPushToken(String expoPushToken);
}
