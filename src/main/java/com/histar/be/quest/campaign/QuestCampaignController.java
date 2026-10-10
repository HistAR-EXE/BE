package com.histar.be.quest.campaign;

import com.histar.be.common.response.ApiResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.histar.be.config.PilotFeatureGate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quest-campaigns")
@RequiredArgsConstructor
public class QuestCampaignController {
    private final QuestCampaignRepository repository;
    private final QuestCampaignAdminService adminService;
    private final com.histar.be.common.security.CurrentUserAccessor currentUser;
    private final PilotFeatureGate pilotFeatureGate;
    private final CampaignAudiencePreviewService audiencePreviewService;

    @GetMapping("/active")
    public ApiResponse<List<QuestCampaignResponse>> active() {
        enabled(); return ApiResponse.ok(repository.findActive(Instant.now()).stream().map(QuestCampaignResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QuestCampaignResponse> create(@RequestBody @jakarta.validation.Valid QuestCampaignRequest request) { enabled(); return ApiResponse.ok(adminService.create(actor(), request)); }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QuestCampaignResponse> update(@PathVariable java.util.UUID id, @RequestBody @jakarta.validation.Valid QuestCampaignRequest request) { enabled(); return ApiResponse.ok(adminService.update(actor(), id, request)); }
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QuestCampaignResponse> publish(@PathVariable java.util.UUID id) { enabled(); return ApiResponse.ok(adminService.setPublished(actor(), id, true)); }
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QuestCampaignResponse> unpublish(@PathVariable java.util.UUID id) { enabled(); return ApiResponse.ok(adminService.setPublished(actor(), id, false)); }
    @GetMapping("/{id}/audit") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<List<QuestCampaignAuditResponse>> audit(@PathVariable java.util.UUID id){ enabled(); return ApiResponse.ok(adminService.history(id)); }
    @PutMapping("/{id}/pilot-gate") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<QuestCampaignResponse> pilotGate(@PathVariable java.util.UUID id, @RequestBody @jakarta.validation.Valid QuestCampaignGateRequest request){ enabled(); return ApiResponse.ok(adminService.updatePilotGate(actor(),id,request)); }
    @PostMapping("/{id}/rollback") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<QuestCampaignResponse> rollback(@PathVariable java.util.UUID id,@RequestBody @jakarta.validation.Valid RollbackCampaignRequest request){ enabled(); return ApiResponse.ok(adminService.rollback(actor(),id,request.reason())); }
    @GetMapping("/{id}/preview-audience") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<CampaignAudiencePreview> preview(@PathVariable java.util.UUID id,@RequestParam java.util.UUID orgId){ enabled(); return ApiResponse.ok(audiencePreviewService.preview(id, orgId)); }
    private java.util.UUID actor() { return currentUser.getUserId().orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized")); }
    private void enabled(){ pilotFeatureGate.requireQuestCampaigns(); }
}
