package com.histar.be.billing.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MultiplayerAccessService {

    private static final Set<String> MULTIPLAYER_PLANS = Set.of("STANDARD", "PREMIUM");

    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;

    public void assertMultiplayerAccess(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElseThrow(() -> new BusinessRuleException("User not found"));
        if (profile.getOrgId() == null) {
            throw new BusinessRuleException(
                    "Multiplayer Experience chỉ dành cho gói B2B Standard/Premium. Liên hệ trường để nâng cấp.");
        }
        Organization org = organizationRepository
                .findById(profile.getOrgId())
                .orElseThrow(() -> new BusinessRuleException("Tổ chức không tồn tại"));
        String plan = org.getPlanType() != null ? org.getPlanType() : org.getPlan();
        if (plan == null || !MULTIPLAYER_PLANS.contains(plan.trim().toUpperCase())) {
            throw new BusinessRuleException(
                    "Multiplayer Experience cần gói Standard hoặc Premium. Gói hiện tại: " + (plan != null ? plan : "MICRO"));
        }
    }

    public boolean hasMultiplayerAccess(UUID userId) {
        try {
            assertMultiplayerAccess(userId);
            return true;
        } catch (BusinessRuleException ex) {
            return false;
        }
    }
}
