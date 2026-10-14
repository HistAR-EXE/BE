package com.histar.be.quest.narrative;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.admin.audit.AdminAuditEventResponse;
import com.histar.be.admin.audit.AdminAuditService;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
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
    private final AdminAuditService auditService;
    private final CurrentUserAccessor currentUser;
    @GetMapping("/{questId}/reviews")
    public ApiResponse<List<NarrativeReviewResponse>> history(@PathVariable UUID questId) { return ApiResponse.ok(service.history(questId)); }
    @PostMapping("/{questId}/reviews")
    public ApiResponse<NarrativeAdminDetailResponse> review(@PathVariable UUID questId, @RequestBody @Valid NarrativeReviewRequest request) {
        String reviewer = currentUser.getEmail().orElseGet(() -> currentUser.getUserId().map(UUID::toString).orElse("unknown"));
        service.review(questId, actorId(), reviewer, request);
        return ApiResponse.ok(pilotAdminService.detail(questId));
    }
    @GetMapping
    public ApiResponse<PageResponse<NarrativePilotListResponse>> listPilots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String rolloutState,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(pilotAdminService.list(page, size, q, reviewStatus, rolloutState, sort));
    }
    @GetMapping("/{questId}")
    public ApiResponse<NarrativeAdminDetailResponse> detail(@PathVariable UUID questId) {
        return ApiResponse.ok(pilotAdminService.detail(questId));
    }
    @GetMapping("/{questId}/readiness")
    public ApiResponse<NarrativeReadinessResponse> readiness(@PathVariable UUID questId,
            @RequestParam(required = false) Integer draftVersion) {
        Integer candidate = draftVersion == null ? pilotAdminService.detail(questId).draftVersion() : draftVersion;
        if (candidate == null) throw new com.histar.be.common.exception.BusinessRuleException("No open narrative draft");
        return ApiResponse.ok(pilotAdminService.readiness(questId, candidate));
    }
    @PostMapping("/{questId}/drafts")
    public ApiResponse<NarrativeAdminDetailResponse> createDraft(@PathVariable UUID questId,
            @RequestBody @Valid NarrativeCreateDraftRequest request) {
        pilotAdminService.createDraft(questId, actorId(), request);
        return ApiResponse.ok(pilotAdminService.detail(questId));
    }
    @PatchMapping("/{questId}/drafts/{version}")
    public ApiResponse<NarrativeAdminDetailResponse> patchDraft(@PathVariable UUID questId, @PathVariable int version,
            @RequestBody @Valid NarrativePatchDraftRequest request) {
        pilotAdminService.patchDraft(questId, version, actorId(), request);
        return ApiResponse.ok(pilotAdminService.detail(questId));
    }
    @GetMapping("/{questId}/draft")
    public ApiResponse<NarrativeDraftResponse> draft(@PathVariable UUID questId) { return ApiResponse.ok(pilotAdminService.draft(questId)); }
    @PutMapping("/{questId}/draft")
    public ApiResponse<NarrativeDraftResponse> updateDraft(@PathVariable UUID questId, @RequestBody @Valid NarrativeDraftUpdateRequest request) {
        if (request.rowVersion() == null) {
            throw new com.histar.be.common.exception.BusinessRuleException("Narrative draft rowVersion is required");
        }
        return ApiResponse.ok(pilotAdminService.updateDraft(questId, actorId(), request));
    }
    @PostMapping("/{questId}/enable")
    public ApiResponse<NarrativeAdminDetailResponse> enable(@PathVariable UUID questId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid NarrativePilotRolloutRequest request) {
        requireRowVersion(request);
        if (request.version() == null) throw new com.histar.be.common.exception.BusinessRuleException("Draft version is required");
        return ApiResponse.ok(pilotAdminService.enable(questId, request.version(), request.rowVersion(), actorId(),
                request.resolvedReason("Phát hành pilot lần đầu"), idempotencyKey));
    }
    @PostMapping("/{questId}/pause")
    public ApiResponse<NarrativeAdminDetailResponse> pause(@PathVariable UUID questId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid NarrativePilotRolloutRequest request) {
        requireRowVersion(request);
        return ApiResponse.ok(pilotAdminService.pause(questId, request.rowVersion(), actorId(),
                request.requiredPauseReason(), idempotencyKey));
    }
    @PostMapping("/{questId}/resume")
    public ApiResponse<NarrativeAdminDetailResponse> resume(@PathVariable UUID questId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid NarrativePilotRolloutRequest request) {
        requireRowVersion(request);
        return ApiResponse.ok(pilotAdminService.resume(questId, request.rowVersion(), actorId(),
                request.resolvedReason("Tiếp tục pilot từ Admin Console"), idempotencyKey));
    }
    @PostMapping("/{questId}/publish-draft")
    public ApiResponse<NarrativeAdminDetailResponse> publishDraft(@PathVariable UUID questId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid NarrativePilotRolloutRequest request) {
        requireRowVersion(request);
        if (request.version() == null) throw new com.histar.be.common.exception.BusinessRuleException("Draft version is required");
        return ApiResponse.ok(pilotAdminService.publishDraft(questId, request.version(), request.rowVersion(), actorId(),
                request.resolvedReason("Phát hành draft mới"), idempotencyKey));
    }
    @PostMapping("/{questId}/disable")
    public ApiResponse<NarrativeAdminDetailResponse> disable(@PathVariable UUID questId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid NarrativePilotRolloutRequest request) {
        requireRowVersion(request);
        return ApiResponse.ok(pilotAdminService.pause(questId, request.rowVersion(), actorId(),
                request.requiredPauseReason(), idempotencyKey));
    }

    @GetMapping("/{questId}/audit")
    public ApiResponse<List<AdminAuditEventResponse>> audit(@PathVariable UUID questId) {
        UUID investigationId = pilotAdminService.detail(questId).investigationId();
        return ApiResponse.ok(auditService.list(0, 100, "NARRATIVE_INVESTIGATION", null, null,
                investigationId, null, null, "occurredAt,desc").getItems());
    }

    private UUID actorId() {
        return currentUser.getUserId().orElseThrow(() -> new NoSuchElementException("Authenticated admin user is required"));
    }

    private void requireRowVersion(NarrativePilotRolloutRequest request) {
        if (request.rowVersion() == null) {
            throw new com.histar.be.common.exception.BusinessRuleException("Narrative rowVersion is required");
        }
    }
}
