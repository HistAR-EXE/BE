package com.histar.be.leaderboard.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.leaderboard.dto.LeaderboardResponse;
import com.histar.be.leaderboard.service.LeaderboardService;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final CurrentUserAccessor currentUserAccessor;
    private final com.histar.be.leaderboard.service.HonorsStoriesService storiesService;
    private final com.histar.be.leaderboard.service.HonorsFactsService factsService;

    @GetMapping
    public ApiResponse<LeaderboardResponse> leaderboard(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID groupId,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        Optional<UUID> userId = currentUserAccessor.getUserId();
        return ApiResponse.ok(leaderboardService.getLeaderboard(scope, city, userId.orElse(null), groupId));
    }
    @GetMapping("/stories")
    public ApiResponse<com.histar.be.leaderboard.dto.HonorsStoriesResponse> stories(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID groupId,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        return ApiResponse.ok(storiesService.read(scope, city, currentUserAccessor.getUserId().orElse(null), groupId));
    }
    @GetMapping("/passport")
    public ApiResponse<com.histar.be.leaderboard.dto.HonorsPassportResponse> passport(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        return ApiResponse.ok(factsService.passport(currentUserAccessor.getUserId().orElse(null)));
    }
    @GetMapping("/community")
    public ApiResponse<com.histar.be.leaderboard.dto.HonorsCommunityResponse> community(
            @RequestParam(defaultValue="all") String scope, @RequestParam(required=false) String city,
            @RequestParam(required=false) UUID groupId, HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        return ApiResponse.ok(factsService.community(scope,city,currentUserAccessor.getUserId().orElse(null),groupId));
    }
}
