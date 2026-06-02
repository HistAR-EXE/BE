package com.histar.be.location.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.secret.dto.SecretStoryResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationSecretController {

    private final GamificationService gamificationService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/{locationId}/secret-story")
    public ApiResponse<SecretStoryResponse> secretStory(@PathVariable UUID locationId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(gamificationService.getSecretStory(userId, locationId));
    }
}
