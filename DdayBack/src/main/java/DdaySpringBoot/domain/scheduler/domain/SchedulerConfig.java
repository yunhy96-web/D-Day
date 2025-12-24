package DdaySpringBoot.domain.scheduler.domain;

import DdaySpringBoot.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "scheduler_configs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchedulerConfig extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no", updatable = false)
    private Long no;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "scheduler_id", nullable = false, unique = true, length = 50)
    private String schedulerId;

    @Column(name = "scheduler_name", nullable = false, length = 100)
    private String schedulerName;

    @Column(name = "cron_expression", length = 50)
    private String cronExpression;

    @Column(name = "fixed_rate_ms")
    private Long fixedRateMs;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "last_crawled_page")
    private Integer lastCrawledPage = 1;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    @Builder
    public SchedulerConfig(String schedulerId, String schedulerName, String cronExpression, Long fixedRateMs, Boolean isEnabled) {
        this.uuid = UUID.randomUUID().toString();
        this.schedulerId = schedulerId;
        this.schedulerName = schedulerName;
        this.cronExpression = cronExpression;
        this.fixedRateMs = fixedRateMs;
        this.isEnabled = isEnabled != null ? isEnabled : true;
    }

    public void toggle() {
        this.isEnabled = !this.isEnabled;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void updateFixedRate(Long fixedRateMs) {
        this.fixedRateMs = fixedRateMs;
    }

    public void updateCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void updateLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public void updateNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public void updateLastCrawledPage(Integer lastCrawledPage) {
        this.lastCrawledPage = lastCrawledPage;
    }

    public void incrementLastCrawledPage() {
        this.lastCrawledPage = (this.lastCrawledPage == null ? 1 : this.lastCrawledPage) + 1;
    }
}
