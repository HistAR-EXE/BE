package com.histar.be.billing.service;

import com.histar.be.billing.OrgPlanLimits;
import com.histar.be.billing.entity.OrgActiveSession;
import com.histar.be.billing.entity.OrgActiveSession.OrgActiveSessionId;
import com.histar.be.billing.repository.OrgActiveSessionRepository;
import com.histar.be.common.exception.CcuLimitException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CcuSessionService {

    private static final int SESSION_TTL_MINUTES = 3;

    private final OrgActiveSessionRepository orgActiveSessionRepository;
    private final OrganizationRepository organizationRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public void heartbeat(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null || profile.getOrgId() == null) {
            return;
        }
        UUID orgId = profile.getOrgId();
        Instant now = Instant.now();
        orgActiveSessionRepository.deleteStale(now.minus(SESSION_TTL_MINUTES, ChronoUnit.MINUTES));

        OrgActiveSessionId id = new OrgActiveSessionId(orgId, userId);
        OrgActiveSession session = orgActiveSessionRepository
                .findById(id)
                .orElse(OrgActiveSession.builder().orgId(orgId).userId(userId).build());
        session.setLastSeenAt(now);
        orgActiveSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public int getCurrentCcu(UUID orgId) {
        Instant since = Instant.now().minus(SESSION_TTL_MINUTES, ChronoUnit.MINUTES);
        return orgActiveSessionRepository.countActiveByOrgId(orgId, since);
    }

    @Transactional
    public void assertCcuAvailable(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null || profile.getOrgId() == null) {
            return;
        }
        UUID orgId = profile.getOrgId();
        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return;
        }
        int maxCcu = org.getMaxCcu() != null
                ? org.getMaxCcu()
                : OrgPlanLimits.forPlan(OrgSubscription.fromPlanType(
                                org.getPlanType() != null ? org.getPlanType() : org.getPlan()))
                        .maxCcu();
        int current = getCurrentCcu(orgId);
        OrgActiveSessionId id = new OrgActiveSessionId(orgId, userId);
        Instant since = Instant.now().minus(SESSION_TTL_MINUTES, ChronoUnit.MINUTES);
        boolean alreadyActive = orgActiveSessionRepository
                .findById(id)
                .map(s -> s.getLastSeenAt() != null && s.getLastSeenAt().isAfter(since))
                .orElse(false);
        if (!alreadyActive && current >= maxCcu) {
            throw new CcuLimitException(
                    "Tổ chức đã đạt giới hạn CCU (" + maxCcu + "). Vui lòng thử lại sau hoặc nâng gói.",
                    maxCcu,
                    current);
        }
        heartbeat(userId);
    }
}
