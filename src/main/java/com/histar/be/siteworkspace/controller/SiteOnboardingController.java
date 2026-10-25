package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.CreateSiteOnboardingRequest;
import com.histar.be.siteworkspace.dto.SiteOnboardingResponse;
import com.histar.be.siteworkspace.service.SiteOnboardingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-onboarding-requests")
@RequiredArgsConstructor
public class SiteOnboardingController {

    private final SiteOnboardingService siteOnboardingService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<SiteOnboardingResponse> submit(@Valid @RequestBody CreateSiteOnboardingRequest request) {
        return ApiResponse.ok(siteOnboardingService.submit(currentUserId(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<SiteOnboardingResponse>> mine() {
        return ApiResponse.ok(siteOnboardingService.listMine(currentUserId()));
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
