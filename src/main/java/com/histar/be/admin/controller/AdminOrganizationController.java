package com.histar.be.admin.controller;

import com.histar.be.admin.dto.AdminOrganizationSummaryResponse;
import com.histar.be.admin.service.AdminOrganizationService;
import com.histar.be.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminOrganizationSummaryResponse>> listOrganizations() {
        return ApiResponse.ok(adminOrganizationService.listAll());
    }
}
