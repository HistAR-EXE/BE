package com.histar.be.profile.service;

import com.histar.be.billing.repository.B2cVisitEntitlementRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
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
    private final B2cVisitEntitlementRepository visitEntitlementRepository;

    /** National Premium / org / staff — unlocks all pilot sites. */
    @Transactional(readOnly = true)
    public boolean hasPremiumAccess(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return false;
        }
        if (profile.getOrgId() != null) {
            return true;
        }
        UserRole role = UserRole.fromStored(profile.getRole());
        if (role == UserRole.ADMIN || role == UserRole.TEACHER) {
            return true;
        }
        return UserTier.PREMIUM == UserTier.fromStored(profile.getTier());
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
