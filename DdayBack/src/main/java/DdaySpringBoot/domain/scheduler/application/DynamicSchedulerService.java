package DdaySpringBoot.domain.scheduler.application;

import DdaySpringBoot.domain.scheduler.domain.SchedulerConfig;
import DdaySpringBoot.domain.scheduler.domain.SchedulerConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicSchedulerService {

    private final TaskScheduler taskScheduler;
    private final SchedulerConfigRepository schedulerConfigRepository;
    private final List<SchedulerTaskRegistry> taskRegistries;

    // 실행 중인 스케줄 관리
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("=== 동적 스케줄러 초기화 시작 ===");
        loadAndStartEnabledSchedulers();
        log.info("=== 동적 스케줄러 초기화 완료 ===");
    }

    @PreDestroy
    public void destroy() {
        log.info("=== 동적 스케줄러 종료 ===");
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();
    }

    /**
     * 활성화된 스케줄러 로드 및 시작
     */
    private void loadAndStartEnabledSchedulers() {
        List<SchedulerConfig> enabledConfigs = schedulerConfigRepository.findByIsEnabledTrue();

        for (SchedulerConfig config : enabledConfigs) {
            startScheduler(config);
        }
    }

    /**
     * 스케줄러 시작
     */
    public void startScheduler(SchedulerConfig config) {
        String schedulerId = config.getSchedulerId();

        // 이미 실행 중이면 중지
        stopScheduler(schedulerId);

        // 태스크 찾기
        SchedulerTaskRegistry task = findTask(schedulerId);
        if (task == null) {
            log.warn("스케줄러 태스크를 찾을 수 없음: {}", schedulerId);
            return;
        }

        ScheduledFuture<?> future;

        if (config.getCronExpression() != null && !config.getCronExpression().isEmpty()) {
            // Cron 표현식 사용
            future = taskScheduler.schedule(
                    () -> executeTask(config, task),
                    new CronTrigger(config.getCronExpression())
            );
            log.info("스케줄러 시작 (Cron): {} - {}", schedulerId, config.getCronExpression());
        } else if (config.getFixedRateMs() != null && config.getFixedRateMs() > 0) {
            // 고정 간격 사용
            future = taskScheduler.scheduleAtFixedRate(
                    () -> executeTask(config, task),
                    config.getFixedRateMs()
            );
            log.info("스케줄러 시작 (FixedRate): {} - {}ms", schedulerId, config.getFixedRateMs());
        } else {
            log.warn("스케줄러 설정이 없음: {}", schedulerId);
            return;
        }

        scheduledTasks.put(schedulerId, future);
    }

    /**
     * 스케줄러 중지
     */
    public void stopScheduler(String schedulerId) {
        ScheduledFuture<?> future = scheduledTasks.remove(schedulerId);
        if (future != null) {
            future.cancel(false);
            log.info("스케줄러 중지: {}", schedulerId);
        }
    }

    /**
     * 태스크 실행
     */
    private void executeTask(SchedulerConfig config, SchedulerTaskRegistry task) {
        try {
            log.info("스케줄러 실행: {}", config.getSchedulerId());
            updateLastRunAt(config.getSchedulerId());
            task.execute();
        } catch (Exception e) {
            log.error("스케줄러 실행 실패: {} - {}", config.getSchedulerId(), e.getMessage(), e);
        }
    }

    /**
     * 마지막 실행 시간 업데이트
     */
    @Transactional
    public void updateLastRunAt(String schedulerId) {
        schedulerConfigRepository.findBySchedulerId(schedulerId)
                .ifPresent(config -> config.updateLastRunAt(LocalDateTime.now()));
    }

    /**
     * 스케줄러 토글 (on/off)
     */
    @Transactional
    public SchedulerConfig toggleScheduler(String schedulerId) {
        SchedulerConfig config = schedulerConfigRepository.findBySchedulerId(schedulerId)
                .orElseThrow(() -> new RuntimeException("스케줄러를 찾을 수 없습니다: " + schedulerId));

        config.toggle();

        if (config.getIsEnabled()) {
            startScheduler(config);
        } else {
            stopScheduler(schedulerId);
        }

        return config;
    }

    /**
     * 스케줄러 활성화
     */
    @Transactional
    public SchedulerConfig enableScheduler(String schedulerId) {
        SchedulerConfig config = schedulerConfigRepository.findBySchedulerId(schedulerId)
                .orElseThrow(() -> new RuntimeException("스케줄러를 찾을 수 없습니다: " + schedulerId));

        config.enable();
        startScheduler(config);

        return config;
    }

    /**
     * 스케줄러 비활성화
     */
    @Transactional
    public SchedulerConfig disableScheduler(String schedulerId) {
        SchedulerConfig config = schedulerConfigRepository.findBySchedulerId(schedulerId)
                .orElseThrow(() -> new RuntimeException("스케줄러를 찾을 수 없습니다: " + schedulerId));

        config.disable();
        stopScheduler(schedulerId);

        return config;
    }

    /**
     * 주기 변경 (밀리초)
     */
    @Transactional
    public SchedulerConfig updateInterval(String schedulerId, Long fixedRateMs) {
        SchedulerConfig config = schedulerConfigRepository.findBySchedulerId(schedulerId)
                .orElseThrow(() -> new RuntimeException("스케줄러를 찾을 수 없습니다: " + schedulerId));

        config.updateFixedRate(fixedRateMs);
        config.updateCronExpression(null); // cron 제거

        if (config.getIsEnabled()) {
            startScheduler(config);
        }

        return config;
    }

    /**
     * Cron 표현식 변경
     */
    @Transactional
    public SchedulerConfig updateCron(String schedulerId, String cronExpression) {
        SchedulerConfig config = schedulerConfigRepository.findBySchedulerId(schedulerId)
                .orElseThrow(() -> new RuntimeException("스케줄러를 찾을 수 없습니다: " + schedulerId));

        config.updateCronExpression(cronExpression);
        config.updateFixedRate(null); // fixedRate 제거

        if (config.getIsEnabled()) {
            startScheduler(config);
        }

        return config;
    }

    /**
     * 모든 스케줄러 설정 조회
     */
    public List<SchedulerConfig> getAllSchedulers() {
        return schedulerConfigRepository.findAll();
    }

    /**
     * 스케줄러 설정 조회
     */
    public SchedulerConfig getScheduler(String schedulerId) {
        return schedulerConfigRepository.findBySchedulerId(schedulerId)
                .orElseThrow(() -> new RuntimeException("스케줄러를 찾을 수 없습니다: " + schedulerId));
    }

    /**
     * 스케줄러 실행 여부 확인
     */
    public boolean isRunning(String schedulerId) {
        ScheduledFuture<?> future = scheduledTasks.get(schedulerId);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    /**
     * 태스크 찾기
     */
    private SchedulerTaskRegistry findTask(String schedulerId) {
        return taskRegistries.stream()
                .filter(task -> task.getSchedulerId().equals(schedulerId))
                .findFirst()
                .orElse(null);
    }
}
