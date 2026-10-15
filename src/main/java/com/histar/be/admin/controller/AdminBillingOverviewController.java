package com.histar.be.admin.controller;

import com.histar.be.billing.dto.AdminBillingOverviewUiResponse;
import com.histar.be.billing.service.AdminFinancialService;
import com.histar.be.common.response.ApiResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing/overview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillingOverviewController {
    private final AdminFinancialService financials;

    @GetMapping
    public ApiResponse<AdminBillingOverviewUiResponse> overview(@RequestParam(defaultValue = "12") int months) {
        return ApiResponse.ok(AdminBillingOverviewUiResponse.from(financials.overview(months), Instant.now().toString()));
    }
}
