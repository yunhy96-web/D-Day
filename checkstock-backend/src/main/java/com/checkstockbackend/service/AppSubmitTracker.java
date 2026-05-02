package com.checkstockbackend.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 앱이 직접 submit-products로 데이터를 보낸 사이트별 최종 시각을 메모리에 보관.
 * 스케줄러가 이 정보를 보고 최근 N분 내 앱 활동이 있는 사이트는 자체 크롤링을 skip한다.
 * (앱 모드와 백엔드 스케줄러 동시 동작으로 인한 이중 푸시 방지)
 */
@Component
public class AppSubmitTracker {

    private static final Duration FRESHNESS = Duration.ofMinutes(3);

    private final Map<String, LocalDateTime> lastSubmitMap = new ConcurrentHashMap<>();

    public void markSubmitted(String siteId) {
        lastSubmitMap.put(siteId, LocalDateTime.now());
    }

    public boolean isRecentlyHandledByApp(String siteId) {
        LocalDateTime last = lastSubmitMap.get(siteId);
        if (last == null) return false;
        return Duration.between(last, LocalDateTime.now()).compareTo(FRESHNESS) < 0;
    }

    public LocalDateTime getLastSubmitAt(String siteId) {
        return lastSubmitMap.get(siteId);
    }
}
