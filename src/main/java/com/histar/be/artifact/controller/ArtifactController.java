package com.histar.be.artifact.controller;

import com.histar.be.artifact.dto.ArtifactResponse;
import com.histar.be.artifact.dto.MyArtifactsResponse;
import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/artifacts")
    public ApiResponse<List<ArtifactResponse>> catalog(@RequestParam UUID locationId) {
        return ApiResponse.ok(artifactService.findCatalog(locationId));
    }

    @GetMapping("/me/artifacts")
    public ApiResponse<MyArtifactsResponse> mine(@RequestParam UUID locationId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(artifactService.findMine(userId, locationId));
    }

    /** @deprecated Client unlock disabled — use discovery flow or check-in rewards. */
    @PostMapping("/me/artifacts/unlock")
    public ApiResponse<Boolean> unlock() {
        currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        throw new AuthException("Manual artifact unlock is disabled. Use discovery or check-in.");
    }
}
