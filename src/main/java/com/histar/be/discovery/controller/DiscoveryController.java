package com.histar.be.discovery.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.discovery.bridge.DiscoveryArtifactBridge;
import com.histar.be.discovery.dto.DiscoveryPointResponse;
import com.histar.be.discovery.dto.DiscoverySummaryResponse;
import com.histar.be.discovery.dto.VisitedLocationsResponse;
import com.histar.be.discovery.dto.RecordDiscoveryRequest;
import com.histar.be.discovery.dto.RecordDiscoveryResponse;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.gamification.dto.QuestProgressSnapshotDto;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import com.histar.be.gamification.service.EngagementOutcomeService;
import com.histar.be.location.service.LocationUnlockService;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.visit.service.VisitSessionService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ProfilePointsService profilePointsService;
    private final EngagementOutcomeService engagementOutcomeService;
    private final LocationUnlockService locationUnlockService;

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

    @GetMapping("/me/discoveries/visited-locations")
    public ApiResponse<VisitedLocationsResponse> visitedLocations() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(discoveryService.visitedLocations(userId));
    }

    @PostMapping("/me/discoveries")
    public ApiResponse<RecordDiscoveryResponse> record(@RequestBody @Valid RecordDiscoveryRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        UUID locationId = request.locationId();
        boolean recorded = discoveryService.record(userId, request.unlockKey(), locationId);
        List<UnlockedArtifactDto> newArtifacts = discoveryArtifactBridge.unlockLinkedArtifacts(userId, request.unlockKey());
        visitSessionService.recordDiscoveryEvent(userId, request.unlockKey(), request.source(), locationId);
        List<QuestCompletedDto> completed =
                questCompletionService.tryComplete(userId, locationId, CompletionTrigger.DISCOVERY);

        int xpEarned = 0;
        if (recorded) {
            xpEarned += profilePointsService.award(userId, ProfilePointsService.XP_DISCOVERY);
        }
        if (!newArtifacts.isEmpty()) {
            xpEarned += profilePointsService.award(
                    userId, ProfilePointsService.XP_ARTIFACT_UNLOCK * newArtifacts.size());
        }

        QuestProgressSnapshotDto questProgress =
                engagementOutcomeService.resolveQuestProgress(userId, locationId, request.unlockKey(), completed);
        var newlyUnlocked = locationUnlockService.resolveNewlyUnlocked(userId, completed);

        return ApiResponse.ok(new RecordDiscoveryResponse(
                recorded, xpEarned, dedupeArtifacts(newArtifacts), questProgress, newlyUnlocked));
    }

    private List<UnlockedArtifactDto> dedupeArtifacts(List<UnlockedArtifactDto> artifacts) {
        Map<UUID, UnlockedArtifactDto> map = new LinkedHashMap<>();
        for (UnlockedArtifactDto artifact : artifacts) {
            map.putIfAbsent(artifact.id(), artifact);
        }
        return new ArrayList<>(map.values());
    }
}
