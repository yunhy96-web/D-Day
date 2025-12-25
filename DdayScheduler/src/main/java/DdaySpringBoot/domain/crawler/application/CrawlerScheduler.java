package DdaySpringBoot.domain.crawler.application;

import DdaySpringBoot.domain.scheduler.application.SchedulerTaskRegistry;
import DdaySpringBoot.domain.scheduler.domain.SchedulerConfig;
import DdaySpringBoot.domain.scheduler.domain.SchedulerConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler implements SchedulerTaskRegistry {

    private final CrawlerService crawlerService;
    private final SchedulerConfigRepository schedulerConfigRepository;

    private static final String SCHEDULER_ID = "CRAWLER_HOTSSUL";

    @Override
    public String getSchedulerId() {
        return SCHEDULER_ID;
    }

    @Override
    @Transactional
    public void execute() {
        log.info("=== 크롤러 스케줄러 실행 ===");
        try {
            // 현재 크롤링할 페이지 조회
            SchedulerConfig config = schedulerConfigRepository.findBySchedulerId(SCHEDULER_ID)
                    .orElseThrow(() -> new RuntimeException("스케줄러 설정을 찾을 수 없음: " + SCHEDULER_ID));

            int currentPage = config.getLastCrawledPage() != null ? config.getLastCrawledPage() : 1;
            log.info("크롤링 페이지: {}", currentPage);

            // 해당 페이지 크롤링 (25개)
            int savedCount = crawlerService.crawlAndSavePage(currentPage);

            // 페이지 번호 증가 및 저장
            config.incrementLastCrawledPage();
            schedulerConfigRepository.save(config);

            log.info("=== 크롤러 스케줄러 완료: 페이지 {}, {}개 글 저장, 다음 페이지: {} ===",
                    currentPage, savedCount, config.getLastCrawledPage());
        } catch (Exception e) {
            log.error("=== 크롤러 스케줄러 에러: {} ===", e.getMessage(), e);
        }
    }
}
