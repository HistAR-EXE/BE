package com.histar.be.minigame.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.minigame.dto.MinigameProgressItem;
import com.histar.be.minigame.dto.MinigameSubmitRequest;
import com.histar.be.minigame.dto.MinigameSubmitResponse;
import com.histar.be.minigame.service.MinigameService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/minigames")
@RequiredArgsConstructor
public class MinigameController {

    private final MinigameService minigameService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/{id}/submit")
    public ApiResponse<MinigameSubmitResponse> submit(
            @PathVariable UUID id, @RequestBody @Valid MinigameSubmitRequest request) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(minigameService.submit(userId, id, request.score()));
    }

    @GetMapping("/me/progress")
    public ApiResponse<List<MinigameProgressItem>> myProgress() {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(minigameService.progressForUser(userId));
    }
}
