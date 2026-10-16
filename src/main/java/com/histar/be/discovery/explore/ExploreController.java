package com.histar.be.discovery.explore;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/me/explore") @RequiredArgsConstructor
public class ExploreController {
    private final ExploreService explore;
    private final CurrentUserAccessor users;
    private UUID userId() { return users.getUserId().orElseThrow(() -> new AuthException("Unauthorized")); }

    @GetMapping public ApiResponse<ExploreService.Snapshot> snapshot() {
        return ApiResponse.ok(explore.snapshot(userId()));
    }
    @PostMapping("/attempts") public ApiResponse<ExploreService.AttemptResponse> submit(
            @RequestBody @Valid ExploreService.AttemptRequest request) {
        return ApiResponse.ok(explore.submit(userId(), request));
    }
    @PostMapping("/experiences") public ApiResponse<ExploreService.Snapshot> experience(
            @RequestBody @Valid ExploreService.ExperienceRequest request) {
        return ApiResponse.ok(explore.experience(userId(), request));
    }
}
