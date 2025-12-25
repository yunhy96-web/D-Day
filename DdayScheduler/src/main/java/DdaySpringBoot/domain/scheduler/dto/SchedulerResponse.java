package DdaySpringBoot.domain.scheduler.dto;

import DdaySpringBoot.domain.scheduler.domain.SchedulerConfig;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SchedulerResponse {
    private final String uuid;
    private final String schedulerId;
    private final String schedulerName;
    private final String cronExpression;
    private final Long fixedRateMs;
    private final Boolean isEnabled;
    private final Boolean isRunning;
    private final LocalDateTime lastRunAt;
    private final LocalDateTime nextRunAt;

    public SchedulerResponse(SchedulerConfig config, boolean isRunning) {
        this.uuid = config.getUuid();
        this.schedulerId = config.getSchedulerId();
        this.schedulerName = config.getSchedulerName();
        this.cronExpression = config.getCronExpression();
        this.fixedRateMs = config.getFixedRateMs();
        this.isEnabled = config.getIsEnabled();
        this.isRunning = isRunning;
        this.lastRunAt = config.getLastRunAt();
        this.nextRunAt = config.getNextRunAt();
    }
}
