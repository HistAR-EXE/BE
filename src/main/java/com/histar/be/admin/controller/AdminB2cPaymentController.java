package com.histar.be.admin.controller;

import com.histar.be.billing.dto.AdminRecentB2cPaymentItem;
import com.histar.be.billing.service.AdminB2cPaymentQueryService;
import com.histar.be.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing/payments")
@RequiredArgsConstructor
public class AdminB2cPaymentController {

    private final AdminB2cPaymentQueryService adminB2cPaymentQueryService;

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminRecentB2cPaymentItem>> recentPayments(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(adminB2cPaymentQueryService.listRecent(limit));
    }

    /** e.g. {@code GET /api/admin/billing/payments?status=UNDERPAID} */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminRecentB2cPaymentItem>> listPayments(
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(adminB2cPaymentQueryService.listByStatus(status, limit));
    }
}
