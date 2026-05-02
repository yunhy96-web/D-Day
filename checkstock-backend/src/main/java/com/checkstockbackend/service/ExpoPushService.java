package com.checkstockbackend.service;

import com.checkstockbackend.dto.ProductDto;
import com.checkstockbackend.entity.DeviceToken;
import com.checkstockbackend.entity.MonitoredSite;
import com.checkstockbackend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final DeviceTokenRepository deviceTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendNewMatchAlert(MonitoredSite site, List<ProductDto> added, int totalMatched) {
        String body;
        if (added.size() == 1) {
            body = "새 상품: " + added.get(0).getName();
        } else {
            body = "새 매칭 상품 " + added.size() + "개 (총 " + totalMatched + "개)";
        }
        String firstUrl = added.isEmpty() ? null : added.get(0).getUrl();
        send(site, body, firstUrl);
    }

    public void sendInStockAlert(MonitoredSite site, List<ProductDto> matched) {
        if (matched.isEmpty()) return;
        String body;
        if (matched.size() == 1) {
            body = "재고 있음: " + matched.get(0).getName();
        } else {
            body = "재고 " + matched.size() + "개: " + matched.get(0).getName() + " 외";
        }
        send(site, body, matched.get(0).getUrl());
    }

    private void send(MonitoredSite site, String body, String firstProductUrl) {
        List<DeviceToken> tokens = deviceTokenRepository.findAll();
        if (tokens.isEmpty()) {
            log.info("등록된 디바이스 토큰이 없어 푸시를 보내지 않습니다.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("siteId", site.getId());
        data.put("siteUrl", site.getUrl());
        if (firstProductUrl != null) {
            data.put("firstProductUrl", firstProductUrl);
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (DeviceToken t : tokens) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("to", t.getExpoPushToken());
            msg.put("title", site.getName());
            msg.put("body", body);
            msg.put("sound", "default");
            msg.put("priority", "high");
            msg.put("_displayInForeground", true);
            msg.put("data", data);
            messages.add(msg);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);

            restTemplate.postForObject(EXPO_PUSH_URL, request, String.class);
            log.info("[{}] Expo Push 전송 완료: {}개 디바이스 - {}", site.getName(), tokens.size(), body);
        } catch (Exception e) {
            log.error("Expo Push 전송 실패: {}", e.getMessage());
        }
    }
}
