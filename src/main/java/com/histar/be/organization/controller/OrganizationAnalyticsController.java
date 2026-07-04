package com.histar.be.organization.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.organization.dto.OrganizationAnalyticsResponse;
import com.histar.be.organization.service.OrganizationAnalyticsService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class OrganizationAnalyticsController {

    private final OrganizationAnalyticsService organizationAnalyticsService;

    @GetMapping("/{orgId}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganizationAnalyticsResponse> analytics(@PathVariable UUID orgId) {
        return ApiResponse.ok(organizationAnalyticsService.analytics(orgId));
    }
}
