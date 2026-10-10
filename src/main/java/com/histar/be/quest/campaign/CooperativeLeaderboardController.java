package com.histar.be.quest.campaign;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.ratelimit.CooperativeLeaderboardRateLimiter;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.PilotFeatureGate;
import com.histar.be.organization.service.OrgAccessService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{orgId}/cooperative-leaderboard")
@RequiredArgsConstructor
public class CooperativeLeaderboardController {
    private final CooperativeLeaderboardService service;
    private final CurrentUserAccessor currentUser;
    private final OrgAccessService access;
    private final CooperativeLeaderboardRateLimiter rateLimiter;
    private final CooperativeLeaderboardAccessAuditRepository auditRepository;
    private final PilotFeatureGate pilotFeatureGate;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<CooperativeLeaderboardEntry>> list(
            @PathVariable UUID orgId, @RequestParam UUID campaignId) {
        pilotFeatureGate.requireQuestCampaigns();
        UUID user = currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        try {
            access.requireOrgAccess(user, orgId);
        } catch (BusinessRuleException exception) {
            auditRepository.save(audit(user, orgId, campaignId, false, "FORBIDDEN", exception.getMessage()));
            throw exception;
        }
        var decision = rateLimiter.tryAcquire(user);
        if (!decision.allowed()) {
            auditRepository.save(audit(user, orgId, campaignId, false, "RATE_LIMITED", "retryAfterSeconds=" + decision.retryAfterSeconds()));
            throw new BusinessRuleException("Đọc leaderboard quá nhanh; thử lại sau " + decision.retryAfterSeconds() + " giây");
        }
        List<CooperativeLeaderboardEntry> result = service.forCampaign(orgId, campaignId);
        auditRepository.save(audit(user, orgId, campaignId, true, "ALLOWED", "entries=" + result.size()));
        return ApiResponse.ok(result);
    }

    private static CooperativeLeaderboardAccessAudit audit(UUID user, UUID orgId, UUID campaignId,
                                                            boolean allowed, String outcome, String detail) {
        return CooperativeLeaderboardAccessAudit.builder().actorId(user).organizationId(orgId).campaignId(campaignId)
                .allowed(allowed).outcome(outcome).occurredAt(Instant.now()).detail(detail).build();
    }
}
