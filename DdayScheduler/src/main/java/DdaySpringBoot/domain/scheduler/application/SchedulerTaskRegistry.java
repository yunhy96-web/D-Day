package DdaySpringBoot.domain.scheduler.application;

/**
 * 스케줄러 태스크 인터페이스
 * 각 스케줄러는 이 인터페이스를 구현해야 함
 */
public interface SchedulerTaskRegistry {

    /**
     * 스케줄러 ID 반환
     */
    String getSchedulerId();

    /**
     * 실제 실행할 태스크
     */
    void execute();
}
