package com.histar.be.billing.controller;

import com.histar.be.billing.dto.BillingStatusResponse;
import com.histar.be.billing.service.BillingService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeQuotaController {

    private final BillingService billingService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/quota")
    public ApiResponse<Map<String, Object>> quota() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        BillingStatusResponse status = billingService.getStatus(userId);
        return ApiResponse.ok(Map.of(
                "usedToday", status.chatUsedToday(),
                "dailyLimit", status.chatDailyLimit(),
                "tier", status.tier(),
                "resetDate", billingService.getUserDailyResetDate()));
    }
}
