package com.histar.be.badge.controller;

import com.histar.be.badge.dto.BadgeResponse;
import com.histar.be.badge.dto.UserBadgeResponse;
import com.histar.be.badge.service.BadgeCatalogService;
import com.histar.be.badge.service.BadgeService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;
    private final BadgeCatalogService badgeCatalogService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/badges")
    public ApiResponse<List<BadgeResponse>> listBadges() {
        List<BadgeResponse> data =
                badgeService.findAll().stream().map(BadgeResponse::from).toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/me/badges")
    public ApiResponse<List<UserBadgeResponse>> myBadges() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(badgeCatalogService.listForUser(userId));
    }
}
