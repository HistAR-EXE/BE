package com.histar.be.admin.controller;

import com.histar.be.admin.dashboard.AdminDashboardResponse;
import com.histar.be.admin.dashboard.AdminDashboardService;
import com.histar.be.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    public ApiResponse<AdminDashboardResponse> get() {
        return ApiResponse.ok(dashboardService.get());
    }
}
