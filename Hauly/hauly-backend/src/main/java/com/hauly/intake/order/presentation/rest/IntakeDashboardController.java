package com.hauly.intake.order.presentation.rest;

import com.hauly.intake.order.application.DashboardQueryService;
import com.hauly.intake.order.application.query.DashboardSummaryView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intake/dashboard")
public class IntakeDashboardController {

    private final DashboardQueryService dashboardQueryService;

    public IntakeDashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryView> summary() {
        return ResponseEntity.ok(dashboardQueryService.summary());
    }
}
