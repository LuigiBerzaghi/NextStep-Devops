package com.softcode.nextstep.api.controller;

import com.softcode.nextstep.api.dto.dashboard.DashboardResponse;
import com.softcode.nextstep.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}

