package com.histar.be.minigame.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.minigame.dto.MinigameResponse;
import com.histar.be.minigame.service.MinigameService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteCode}/stations/{stationCode}/games")
@RequiredArgsConstructor
public class StationMinigameController {

    private final MinigameService minigameService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<List<MinigameResponse>> list(
            @PathVariable String siteCode, @PathVariable String stationCode) {
        UUID userId = currentUserAccessor.getUserId().orElse(null);
        return ApiResponse.ok(minigameService.listForStation(siteCode, stationCode, userId));
    }
}
