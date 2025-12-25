package DdaySpringBoot.domain.scheduler.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchedulerConfigRepository extends JpaRepository<SchedulerConfig, Long> {

    Optional<SchedulerConfig> findBySchedulerId(String schedulerId);

    Optional<SchedulerConfig> findByUuid(String uuid);

    List<SchedulerConfig> findByIsEnabledTrue();
}
