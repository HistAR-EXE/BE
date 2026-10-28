package com.histar.be.profile.service;

import com.histar.be.billing.repository.B2cVisitEntitlementRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.organization.service.OrganizationEntitlementService;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TierAccessService {

    private final ProfileRepository profileRepository;
    private final com.histar.be.billing.repository.B2cSubscriptionRepository b2cSubscriptionRepository;
    private final B2cVisitEntitlementRepository visitEntitlementRepository;
    private final OrganizationEntitlementService organizationEntitlements;

    /** National Premium / org / staff — unlocks all pilot sites. */
    @Transactional(readOnly = true)
    public boolean hasPremiumAccess(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return false;
        }
        UserRole role = UserRole.fromStored(profile.getRole());
        if (role == UserRole.ADMIN || hasPersonalPremiumAccess(profile)) {
            return true;
        }
        return organizationEntitlements.hasActiveMembership(profile);
    }

    /** Paid history is authoritative; no-history legacy grants retain their existing tier. */
    @Transactional(readOnly = true)
    public boolean hasPersonalPremiumAccess(Profile profile) {
        if (profile == null || profile.getId() == null) return false;
        var history = b2cSubscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(profile.getId());
        if (history.isEmpty()) return UserTier.PREMIUM == UserTier.fromStored(profile.getTier());
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        return history.stream().anyMatch(subscription -> Boolean.TRUE.equals(subscription.getIsActive())
                && subscription.getStartDate() != null && !subscription.getStartDate().isAfter(today)
                && subscription.getEndDate() != null && !subscription.getEndDate().isBefore(today));
    }

    /**
     * Story chapters 3–6 + chat citations: Premium OR active Journey Pass for {@code siteCode}.
     * When {@code siteCode} is blank, Journey Pass does not apply (Premium-only).
     */
    @Transactional(readOnly = true)
    public boolean hasStoryAccess(UUID userId, String siteCode) {
        if (hasPremiumAccess(userId)) {
            return true;
        }
        if (siteCode == null || siteCode.isBlank()) {
            return false;
        }
        String site = siteCode.trim().toLowerCase(Locale.ROOT);
        return visitEntitlementRepository.existsByUserIdAndSiteCodeAndExpiresAtAfter(userId, site, Instant.now());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveJourneyPass(UUID userId, String siteCode) {
        if (userId == null || siteCode == null || siteCode.isBlank()) {
            return false;
        }
        return visitEntitlementRepository.existsByUserIdAndSiteCodeAndExpiresAtAfter(
                userId, siteCode.trim().toLowerCase(Locale.ROOT), Instant.now());
    }
}
