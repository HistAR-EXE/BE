package com.histar.be.organization.service;

import com.histar.be.billing.service.BillingService;
import com.histar.be.common.exception.TrialExpiredException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrialEntitlementGuard {

    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public void assertStudentCanWrite(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null || profile.getOrgId() == null) {
            return;
        }
        Organization org = organizationRepository.findById(profile.getOrgId()).orElse(null);
        if (org == null) {
            return;
        }
        if (!BillingService.ORG_STATUS_TRIAL_EXPIRED.equalsIgnoreCase(org.getStatus())) {
            return;
        }
        boolean isStudent = organizationMemberRepository
                .findByOrganizationIdAndUserId(profile.getOrgId(), userId)
                .map(m -> "student".equalsIgnoreCase(m.getOrgRole()))
                .orElse(false);
        if (isStudent) {
            throw new TrialExpiredException(
                    "Gói thử nghiệm lớp học đã hết hạn. Vui lòng nâng cấp để tiếp tục làm Quest.");
        }
    }
}
