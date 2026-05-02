package com.checkstock.scheduler;

import com.checkstock.dto.SizeOptionDto;
import com.checkstock.entity.Product;
import com.checkstock.repository.ProductRepository;
import com.checkstock.service.CrawlerService;
import com.checkstock.service.MonitoringService;
import com.checkstock.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class StockScheduler {

    private final ProductRepository productRepository;
    private final CrawlerService crawlerService;
    private final MonitoringService monitoringService;
    private final TelegramService telegramService;

    // 상품별 다음 체크 예정 시간 (지터 포함)
    private final Map<String, LocalDateTime> nextCheckMap = new ConcurrentHashMap<>();

    // 상품별 연속 에러 횟수 추적
    private final Map<String, Integer> errorCountMap = new ConcurrentHashMap<>();

    // 에러 알림을 보내는 연속 에러 횟수 임계치
    private static final int ERROR_ALERT_THRESHOLD = 5;

    // 봇 탐지 회피용 하한값
    private static final int MIN_REFRESH_INTERVAL_SEC = 180;
    private static final int REFRESH_JITTER_SEC = 60;       // ± 60초
    private static final int INTER_PRODUCT_SLEEP_MIN_MS = 30_000;
    private static final int INTER_PRODUCT_SLEEP_MAX_MS = 60_000;

    @Scheduled(fixedRate = 10000) // 10초마다 체크 (상품별 interval 확인)
    public void checkProducts() {
        List<Product> activeProducts = productRepository.findByActiveTrue();

        for (Product product : activeProducts) {
            LocalDateTime nextCheck = nextCheckMap.get(product.getId());
            LocalDateTime now = LocalDateTime.now();

            // 상품별 다음 체크 시간 도달 여부
            if (nextCheck != null && nextCheck.isAfter(now)) {
                continue; // 아직 주기 안 됨
            }

            try {
                log.info("[{}] 크롤링 시작...", product.getName());
                List<SizeOptionDto> sizes = crawlerService.crawl(product);
                monitoringService.process(product, sizes);
                nextCheckMap.put(product.getId(), now.plusSeconds(computeNextIntervalSec(product)));
                log.info("[{}] 크롤링 완료. 사이즈 {}개", product.getName(), sizes.size());

                // 성공하면 에러 카운트 리셋
                errorCountMap.remove(product.getId());

                // 상품 간 간격 (봇 감지 방지) - 30~60초 랜덤
                Thread.sleep(ThreadLocalRandom.current().nextInt(INTER_PRODUCT_SLEEP_MIN_MS, INTER_PRODUCT_SLEEP_MAX_MS));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[{}] 크롤링 실패: {}", product.getName(), e.getMessage());

                // 연속 에러 카운트 증가
                int errorCount = errorCountMap.merge(product.getId(), 1, Integer::sum);

                // 실패 시에도 다음 체크 시간 설정 + 지수 백오프 (최대 30분)
                long backoffSec = Math.min(errorCount * 300L, 1800L);
                nextCheckMap.put(product.getId(), now.plusSeconds(computeNextIntervalSec(product) + backoffSec));

                // 임계치 도달 시 텔레그램 알림 (이후 매 5회마다 반복)
                if (errorCount == ERROR_ALERT_THRESHOLD || (errorCount > ERROR_ALERT_THRESHOLD && errorCount % ERROR_ALERT_THRESHOLD == 0)) {
                    log.warn("[{}] 연속 {}회 크롤링 실패 → 텔레그램 에러 알림 발송", product.getName(), errorCount);
                    telegramService.sendErrorAlert(product.getName(), e.getMessage());
                }
            }
        }
    }

    private int computeNextIntervalSec(Product product) {
        int base = Math.max(product.getRefreshInterval(), MIN_REFRESH_INTERVAL_SEC);
        int jitter = ThreadLocalRandom.current().nextInt(-REFRESH_JITTER_SEC, REFRESH_JITTER_SEC + 1);
        return base + jitter;
    }
}
