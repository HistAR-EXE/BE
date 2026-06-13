package com.histar.be.discovery.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.discovery.bridge.DiscoveryArtifactBridge;
import com.histar.be.discovery.dto.DiscoveryPointResponse;
import com.histar.be.discovery.dto.DiscoverySummaryResponse;
import com.histar.be.discovery.dto.RecordDiscoveryRequest;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.visit.service.VisitSessionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final QuestCompletionService questCompletionService;
    private final DiscoveryArtifactBridge discoveryArtifactBridge;
    private final CurrentUserAccessor currentUserAccessor;
    private final VisitSessionService visitSessionService;

    @GetMapping("/discovery-points/by-location/{locationId}")
    public ApiResponse<List<DiscoveryPointResponse>> points(@PathVariable UUID locationId) {
        return ApiResponse.ok(discoveryService.findPointsByLocation(locationId));
    }

    @GetMapping("/me/discoveries/summary")
    public ApiResponse<DiscoverySummaryResponse> summary(@RequestParam UUID locationId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(discoveryService.summary(userId, locationId));
    }

    @PostMapping("/me/discoveries")
    public ApiResponse<Boolean> record(@RequestBody @Valid RecordDiscoveryRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        UUID locationId = request.locationId();
        boolean recorded = discoveryService.record(userId, request.unlockKey(), locationId);
        discoveryArtifactBridge.unlockLinkedArtifacts(userId, request.unlockKey());
        visitSessionService.recordDiscoveryEvent(userId, request.unlockKey(), request.source(), locationId);
        questCompletionService.tryComplete(userId, locationId, CompletionTrigger.DISCOVERY);
        return ApiResponse.ok(recorded);
    }
}
