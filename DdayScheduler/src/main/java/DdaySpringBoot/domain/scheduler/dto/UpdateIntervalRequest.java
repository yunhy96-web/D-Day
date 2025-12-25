package DdaySpringBoot.domain.scheduler.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateIntervalRequest {

    @Min(value = 1000, message = "최소 1000ms (1초) 이상이어야 합니다")
    private Long fixedRateMs;
}
