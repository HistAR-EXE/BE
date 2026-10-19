package com.histar.be.discovery.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.discovery.dto.DiscoveryPointResponse;
import com.histar.be.discovery.dto.DiscoverySummaryResponse;
import com.histar.be.discovery.dto.VisitedLocationsResponse;
import com.histar.be.discovery.dto.RecordDiscoveryRequest;
import com.histar.be.discovery.dto.RecordDiscoveryResponse;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.organization.service.TrialEntitlementGuard;
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
    private final CurrentUserAccessor currentUserAccessor;
    private final TrialEntitlementGuard trialEntitlementGuard;
    private final com.histar.be.discovery.service.DiscoveryRewardService discoveryRewards;
    private final com.histar.be.discovery.service.VerifiedDialogueService verifiedDialogue;
    private final com.histar.be.discovery.service.DirectDiscoveryPolicy directDiscoveryPolicy;

    @PostMapping("/me/discoveries/dialogue")
    public ApiResponse<RecordDiscoveryResponse> dialogue(
            @RequestBody @Valid com.histar.be.discovery.service.VerifiedDialogueService.Request request) {
        UUID userId = currentUserAccessor.getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(verifiedDialogue.complete(userId, request));
    }

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
        trialEntitlementGuard.assertStudentCanWrite(userId);
        String key = request.unlockKey();
        // This endpoint records viewing only. Challenges and server events have dedicated validators.
        if (!directDiscoveryPolicy.allows(request.locationId(), key)) {
            throw new com.histar.be.common.exception.BusinessRuleException(
                    "Hoạt động này cần được xác nhận qua luồng thử thách tương ứng.");
        }
        return ApiResponse.ok(discoveryRewards.record(userId, request.locationId(), key, request.source()));
    }

}
