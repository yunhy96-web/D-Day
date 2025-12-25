package DdaySpringBoot.domain.scheduler.presentation;

import DdaySpringBoot.domain.scheduler.application.DynamicSchedulerService;
import DdaySpringBoot.domain.scheduler.domain.SchedulerConfig;
import DdaySpringBoot.domain.scheduler.dto.SchedulerResponse;
import DdaySpringBoot.domain.scheduler.dto.UpdateIntervalRequest;
import DdaySpringBoot.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Scheduler Admin", description = "스케줄러 관리 API")
@RestController
@RequestMapping("/api/admin/schedulers")
@RequiredArgsConstructor
public class SchedulerAdminController {

    private final DynamicSchedulerService schedulerService;

    @Operation(summary = "모든 스케줄러 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SchedulerResponse>>> getAllSchedulers() {
        List<SchedulerResponse> schedulers = schedulerService.getAllSchedulers().stream()
                .map(config -> new SchedulerResponse(config, schedulerService.isRunning(config.getSchedulerId())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(schedulers));
    }

    @Operation(summary = "스케줄러 상세 조회")
    @GetMapping("/{schedulerId}")
    public ResponseEntity<ApiResponse<SchedulerResponse>> getScheduler(@PathVariable String schedulerId) {
        SchedulerConfig config = schedulerService.getScheduler(schedulerId);
        return ResponseEntity.ok(ApiResponse.success(
                new SchedulerResponse(config, schedulerService.isRunning(schedulerId))
        ));
    }

    @Operation(summary = "스케줄러 토글 (on/off)")
    @PutMapping("/{schedulerId}/toggle")
    public ResponseEntity<ApiResponse<SchedulerResponse>> toggleScheduler(@PathVariable String schedulerId) {
        SchedulerConfig config = schedulerService.toggleScheduler(schedulerId);
        return ResponseEntity.ok(ApiResponse.success(
                config.getIsEnabled() ? "스케줄러 활성화" : "스케줄러 비활성화",
                new SchedulerResponse(config, schedulerService.isRunning(schedulerId))
        ));
    }

    @Operation(summary = "스케줄러 활성화")
    @PutMapping("/{schedulerId}/enable")
    public ResponseEntity<ApiResponse<SchedulerResponse>> enableScheduler(@PathVariable String schedulerId) {
        SchedulerConfig config = schedulerService.enableScheduler(schedulerId);
        return ResponseEntity.ok(ApiResponse.success(
                "스케줄러 활성화",
                new SchedulerResponse(config, schedulerService.isRunning(schedulerId))
        ));
    }

    @Operation(summary = "스케줄러 비활성화")
    @PutMapping("/{schedulerId}/disable")
    public ResponseEntity<ApiResponse<SchedulerResponse>> disableScheduler(@PathVariable String schedulerId) {
        SchedulerConfig config = schedulerService.disableScheduler(schedulerId);
        return ResponseEntity.ok(ApiResponse.success(
                "스케줄러 비활성화",
                new SchedulerResponse(config, schedulerService.isRunning(schedulerId))
        ));
    }

    @Operation(summary = "스케줄러 주기 변경 (밀리초)")
    @PutMapping("/{schedulerId}/interval")
    public ResponseEntity<ApiResponse<SchedulerResponse>> updateInterval(
            @PathVariable String schedulerId,
            @Valid @RequestBody UpdateIntervalRequest request) {
        SchedulerConfig config = schedulerService.updateInterval(schedulerId, request.getFixedRateMs());
        return ResponseEntity.ok(ApiResponse.success(
                "주기 변경 완료: " + request.getFixedRateMs() + "ms",
                new SchedulerResponse(config, schedulerService.isRunning(schedulerId))
        ));
    }
}
