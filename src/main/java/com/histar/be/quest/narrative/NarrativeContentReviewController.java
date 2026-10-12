package com.histar.be.quest.narrative;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/narrative-investigations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NarrativeContentReviewController {
    private final NarrativeContentReviewService service;
    private final NarrativePilotAdminService pilotAdminService;
    private final CurrentUserAccessor currentUser;
    @GetMapping("/{questId}/reviews")
    public ApiResponse<List<NarrativeReviewResponse>> history(@PathVariable UUID questId) { return ApiResponse.ok(service.history(questId)); }
    @PostMapping("/{questId}/reviews")
    public ApiResponse<NarrativeReviewResponse> review(@PathVariable UUID questId, @RequestBody @Valid NarrativeReviewRequest request) {
        String reviewer = currentUser.getEmail().orElseGet(() -> currentUser.getUserId().map(UUID::toString).orElse("unknown"));
        return ApiResponse.ok(service.review(questId, reviewer, request));
    }
    @GetMapping
    public ApiResponse<List<NarrativePilotAdminResponse>> listPilots() { return ApiResponse.ok(pilotAdminService.list()); }
    @GetMapping("/{questId}/readiness")
    public ApiResponse<NarrativePilotAdminResponse> readiness(@PathVariable UUID questId) { return ApiResponse.ok(pilotAdminService.readiness(questId)); }
    @GetMapping("/{questId}/draft")
    public ApiResponse<NarrativeDraftResponse> draft(@PathVariable UUID questId) { return ApiResponse.ok(pilotAdminService.draft(questId)); }
    @PutMapping("/{questId}/draft")
    public ApiResponse<NarrativeDraftResponse> updateDraft(@PathVariable UUID questId, @RequestBody @Valid NarrativeDraftUpdateRequest request) {
        return ApiResponse.ok(pilotAdminService.updateDraft(questId, currentUser.getUserId().orElse(null), request));
    }
    @PostMapping("/{questId}/enable")
    public ApiResponse<NarrativePilotAdminResponse> enable(@PathVariable UUID questId, @RequestBody @Valid NarrativePilotRolloutRequest request) {
        return ApiResponse.ok(pilotAdminService.enable(questId, currentUser.getUserId().orElse(null), request.reason()));
    }
    @PostMapping("/{questId}/disable")
    public ApiResponse<NarrativePilotAdminResponse> disable(@PathVariable UUID questId, @RequestBody @Valid NarrativePilotRolloutRequest request) {
        return ApiResponse.ok(pilotAdminService.disable(questId, currentUser.getUserId().orElse(null), request.reason()));
    }
}
