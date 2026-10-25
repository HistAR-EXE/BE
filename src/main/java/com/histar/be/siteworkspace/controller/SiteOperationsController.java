package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteOperationsSummaryResponse;
import com.histar.be.siteworkspace.service.SiteOperationsAnalyticsService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-workspaces")
@RequiredArgsConstructor
public class SiteOperationsController {

    private final SiteOperationsAnalyticsService siteOperationsAnalyticsService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/{siteCode}/operations")
    public ApiResponse<SiteOperationsSummaryResponse> operations(
            @PathVariable String siteCode,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ApiResponse.ok(siteOperationsAnalyticsService.summary(currentUserId(), siteCode, from, to));
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
