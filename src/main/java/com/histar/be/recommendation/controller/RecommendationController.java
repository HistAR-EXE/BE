package com.histar.be.recommendation.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.recommendation.dto.RecommendationsResponse;
import com.histar.be.recommendation.service.RecommendationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<RecommendationsResponse> list(
            @RequestParam UUID locationId,
            @RequestParam(required = false) String afterUnlockKey) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(recommendationService.forUser(userId, locationId, afterUnlockKey));
    }
}
