package com.histar.be.profile.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.billing.service.BillingService;
import com.histar.be.profile.dto.ProfileMeResponse;
import com.histar.be.profile.dto.UpdateProfileRequest;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.service.ProfileMeService;
import com.histar.be.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileMeService profileMeService;
    private final BillingService billingService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/me")
    public ApiResponse<ProfileMeResponse> me() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(profileMeService.build(profileService.findById(userId)));
    }

    @PatchMapping("/me")
    public ApiResponse<ProfileMeResponse> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        Profile profile = profileService.findById(userId);
        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName().trim());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl().trim());
        }
        if (request.city() != null) {
            profile.setCity(request.city().trim());
        }
        Profile saved = profileService.save(profile);
        return ApiResponse.ok(profileMeService.build(saved));
    }

    @PostMapping("/upgrade")
    public ApiResponse<ProfileMeResponse> upgrade() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(billingService.subscribeB2c(userId, "DEMO"));
    }
}
