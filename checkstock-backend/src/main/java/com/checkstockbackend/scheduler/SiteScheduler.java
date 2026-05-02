package com.checkstockbackend.scheduler;

import com.checkstockbackend.dto.ProductDto;
import com.checkstockbackend.entity.MonitoredSite;
import com.checkstockbackend.repository.MonitoredSiteRepository;
import com.checkstockbackend.service.AppSubmitTracker;
import com.checkstockbackend.service.MonitoringService;
import com.checkstockbackend.service.SiteCrawlerService;
import com.checkstockbackend.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteScheduler {

    private final MonitoredSiteRepository siteRepository;
    private final SiteCrawlerService crawlerService;
    private final MonitoringService monitoringService;
    private final TelegramService telegramService;
    private final AppSubmitTracker appSubmitTracker;

    private final Map<String, LocalDateTime> nextCheckMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> errorCountMap = new ConcurrentHashMap<>();

    private static final int ERROR_ALERT_THRESHOLD = 5;
    private static final int MIN_INTERVAL_SEC = 600;
    private static final int JITTER_SEC = 120;

    @Value("${checkstock.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Scheduled(fixedRate = 10000) // 10초마다 체크
    public void checkSites() {
        if (!schedulerEnabled) return;
        List<MonitoredSite> sites = siteRepository.findByActiveTrue();
        LocalDateTime now = LocalDateTime.now();

        for (MonitoredSite site : sites) {
            LocalDateTime nextCheck = nextCheckMap.get(site.getId());
            if (nextCheck != null && nextCheck.isAfter(now)) continue;

            if (appSubmitTracker.isRecentlyHandledByApp(site.getId())) {
                log.info("[{}] 앱이 최근 직접 모니터링 중 → 스케줄러 크롤 skip", site.getName());
                nextCheckMap.put(site.getId(), now.plusSeconds(computeNextIntervalSec(site)));
                continue;
            }

            try {
                log.info("[{}] 크롤링 시작...", site.getName());
                List<ProductDto> products = crawlerService.crawl(site);
                monitoringService.process(site, products);

                nextCheckMap.put(site.getId(), now.plusSeconds(computeNextIntervalSec(site)));
                errorCountMap.remove(site.getId());
            } catch (Exception e) {
                log.error("[{}] 크롤링 실패: {}", site.getName(), e.getMessage());

                int errorCount = errorCountMap.merge(site.getId(), 1, Integer::sum);
                long backoffSec = Math.min(errorCount * 300L, 1800L);
                nextCheckMap.put(site.getId(), now.plusSeconds(computeNextIntervalSec(site) + backoffSec));

                if (errorCount == ERROR_ALERT_THRESHOLD
                        || (errorCount > ERROR_ALERT_THRESHOLD && errorCount % ERROR_ALERT_THRESHOLD == 0)) {
                    log.warn("[{}] 연속 {}회 실패 → 텔레그램 에러 알림", site.getName(), errorCount);
                    telegramService.sendErrorAlert(site.getName(), e.getMessage());
                }
            }
        }
    }

    private int computeNextIntervalSec(MonitoredSite site) {
        int base = Math.max(site.getRefreshIntervalSec(), MIN_INTERVAL_SEC);
        int jitter = ThreadLocalRandom.current().nextInt(-JITTER_SEC, JITTER_SEC + 1);
        return base + jitter;
    }
}
