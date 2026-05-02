package com.checkstockbackend.controller;

import com.checkstockbackend.dto.DeviceTokenRequest;
import com.checkstockbackend.entity.DeviceToken;
import com.checkstockbackend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenController {

    private final DeviceTokenRepository repository;

    @PostMapping
    public ResponseEntity<Map<String, String>> register(@RequestBody DeviceTokenRequest req) {
        if (req.getExpoPushToken() == null || req.getExpoPushToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "expoPushToken required"));
        }

        Optional<DeviceToken> existing = repository.findByExpoPushToken(req.getExpoPushToken());
        DeviceToken token = existing.orElseGet(DeviceToken::new);
        token.setExpoPushToken(req.getExpoPushToken());
        token.setPlatform(req.getPlatform());
        token.setLastSeenAt(LocalDateTime.now());
        repository.save(token);

        log.info("디바이스 토큰 등록/갱신: {} ({})",
                req.getExpoPushToken().substring(0, Math.min(20, req.getExpoPushToken().length())),
                req.getPlatform());
        return ResponseEntity.ok(Map.of("id", token.getId()));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "count", String.valueOf(repository.count()));
    }
}
