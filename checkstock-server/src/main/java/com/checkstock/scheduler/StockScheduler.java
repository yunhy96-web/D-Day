package com.checkstock.scheduler;

import com.checkstock.dto.SizeOptionDto;
import com.checkstock.entity.Product;
import com.checkstock.repository.ProductRepository;
import com.checkstock.service.CrawlerService;
import com.checkstock.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockScheduler {

    private final ProductRepository productRepository;
    private final CrawlerService crawlerService;
    private final MonitoringService monitoringService;

    // 상품별 마지막 체크 시간 기록
    private final Map<String, LocalDateTime> lastCheckedMap = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 10000) // 10초마다 체크 (상품별 interval 확인)
    public void checkProducts() {
        List<Product> activeProducts = productRepository.findByActiveTrue();

        for (Product product : activeProducts) {
            LocalDateTime lastChecked = lastCheckedMap.get(product.getId());
            LocalDateTime now = LocalDateTime.now();

            // 상품별 refreshInterval 체크
            if (lastChecked != null &&
                    lastChecked.plusSeconds(product.getRefreshInterval()).isAfter(now)) {
                continue; // 아직 주기 안 됨
            }

            try {
                log.info("[{}] 크롤링 시작...", product.getName());
                List<SizeOptionDto> sizes = crawlerService.crawl(product);
                monitoringService.process(product, sizes);
                lastCheckedMap.put(product.getId(), now);
                log.info("[{}] 크롤링 완료. 사이즈 {}개", product.getName(), sizes.size());

                // 상품 간 간격 (봇 감지 방지)
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[{}] 크롤링 실패: {}", product.getName(), e.getMessage());
            }
        }
    }
}
