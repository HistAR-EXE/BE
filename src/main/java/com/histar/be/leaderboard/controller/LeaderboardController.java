package com.histar.be.leaderboard.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.leaderboard.dto.LeaderboardResponse;
import com.histar.be.leaderboard.service.LeaderboardService;
import java.util.Optional;
import java.util.UUID;
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

    @GetMapping
    public ApiResponse<LeaderboardResponse> leaderboard(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID groupId) {
        Optional<UUID> userId = currentUserAccessor.getUserId();
        return ApiResponse.ok(leaderboardService.getLeaderboard(scope, city, userId.orElse(null), groupId));
    }
}
