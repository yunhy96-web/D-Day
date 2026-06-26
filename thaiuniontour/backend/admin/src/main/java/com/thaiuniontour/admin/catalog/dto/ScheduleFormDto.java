package com.thaiuniontour.admin.catalog.dto;

import com.thaiuniontour.domain.catalog.model.ScheduleStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ScheduleFormDto {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate saleDate;

    /** 빈값 = 무제한(null). */
    private Integer stock;

    private long price;

    private ScheduleStatus status = ScheduleStatus.OPEN;
}
