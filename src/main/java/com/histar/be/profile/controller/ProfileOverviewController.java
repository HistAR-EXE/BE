package com.histar.be.profile.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.profile.dto.ProfileOverviewResponse;
import com.histar.be.profile.service.ProfileOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/overview")
@RequiredArgsConstructor
public class ProfileOverviewController {
    private final CurrentUserAccessor currentUser;
    private final ProfileOverviewService overview;

    @GetMapping
    public ApiResponse<ProfileOverviewResponse> me() {
        return ApiResponse.ok(overview.overview(currentUser.getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"))));
    }
}
