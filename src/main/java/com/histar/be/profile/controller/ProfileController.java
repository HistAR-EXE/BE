package com.histar.be.profile.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.GamificationProperties;
import com.histar.be.profile.dto.ProfileMeResponse;
import com.histar.be.profile.dto.UpdateProfileRequest;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserAccessor currentUserAccessor;
    private final GamificationProperties gamificationProperties;

    @GetMapping("/me")
    public ApiResponse<ProfileMeResponse> me() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(
                ProfileMeResponse.from(profileService.findById(userId), gamificationProperties));
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
        return ApiResponse.ok(ProfileMeResponse.from(saved, gamificationProperties));
    }
}
