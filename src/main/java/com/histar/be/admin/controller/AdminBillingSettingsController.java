package com.histar.be.admin.controller;

import com.histar.be.billing.dto.AdminBillingSettingsResponse;
import com.histar.be.billing.dto.UpdateBillingSettingsRequest;
import com.histar.be.billing.service.BillingSettingsService;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing/settings")
@RequiredArgsConstructor
public class AdminBillingSettingsController {

    private final BillingSettingsService billingSettingsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminBillingSettingsResponse> getSettings() {
        return ApiResponse.ok(billingSettingsService.getAdminSettings());
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminBillingSettingsResponse> updateSettings(
            @RequestBody @Valid UpdateBillingSettingsRequest request) {
        return ApiResponse.ok(billingSettingsService.updateSettings(request));
    }
}
