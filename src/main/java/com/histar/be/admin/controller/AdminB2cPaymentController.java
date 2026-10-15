package com.histar.be.admin.controller;

import com.histar.be.billing.dto.AdminRecentB2cPaymentItem;
import com.histar.be.billing.service.AdminB2cPaymentQueryService;
import com.histar.be.billing.service.AdminPaymentQueryService;
import com.histar.be.common.response.ApiResponse;
import java.util.List;
import java.time.Instant;
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
    private final AdminPaymentQueryService adminPaymentQueryService;

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminRecentB2cPaymentItem>> recentPayments(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(adminB2cPaymentQueryService.listRecent(limit));
    }

    /** e.g. {@code GET /api/admin/billing/payments?status=UNDERPAID} */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> listPayments(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "50") int limit) {
        if (page == null) {
            return ApiResponse.ok(adminB2cPaymentQueryService.listByStatus(status, limit));
        }
        return ApiResponse.ok(adminPaymentQueryService.search(page, size, channel, status, from, to, sort));
    }
}
