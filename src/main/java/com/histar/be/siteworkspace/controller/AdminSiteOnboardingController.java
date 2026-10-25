package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.ReviewSiteOnboardingRequest;
import com.histar.be.siteworkspace.dto.SiteOnboardingResponse;
import com.histar.be.siteworkspace.entity.SiteOnboardingStatus;
import com.histar.be.siteworkspace.service.SiteOnboardingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/site-onboarding-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteOnboardingController {

    private final SiteOnboardingService siteOnboardingService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<List<SiteOnboardingResponse>> list(
            @RequestParam(name = "status", defaultValue = "PENDING") SiteOnboardingStatus status) {
        return ApiResponse.ok(siteOnboardingService.listByStatus(status));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<SiteOnboardingResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ReviewSiteOnboardingRequest request) {
        return ApiResponse.ok(siteOnboardingService.approve(id, currentUserId(), note(request)));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<SiteOnboardingResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewSiteOnboardingRequest request) {
        return ApiResponse.ok(siteOnboardingService.reject(id, currentUserId(), note(request)));
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }

    private static String note(ReviewSiteOnboardingRequest request) {
        return request == null ? null : request.note();
    }
}
