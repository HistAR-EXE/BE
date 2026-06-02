package com.histar.be.profile.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.profile.dto.ProfileMeResponse;
import com.histar.be.profile.service.ProfileService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/me")
    public ApiResponse<ProfileMeResponse> me() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(ProfileMeResponse.from(profileService.findById(userId)));
    }
}
