package com.histar.be.profile.service;

import com.histar.be.config.GamificationProperties;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.dto.ProfileMeResponse;
import com.histar.be.profile.entity.Profile;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileMeService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final GamificationProperties gamificationProperties;

    @Transactional(readOnly = true)
    public ProfileMeResponse build(Profile profile) {
        UUID orgId = profile.getOrgId();
        String orgName = null;
        String orgRole = null;
        String orgSubscription = profile.getOrgSubscription() != null
                ? OrgSubscription.fromStored(profile.getOrgSubscription()).name()
                : OrgSubscription.NONE.name();

        if (orgId != null) {
            Organization org = organizationRepository.findById(orgId).orElse(null);
            if (org != null) {
                orgName = org.getName();
                orgSubscription = OrgSubscription.fromOrgPlan(org.getPlan()).name();
            }
            orgRole = organizationMemberRepository
                    .findByOrganizationIdAndUserId(orgId, profile.getId())
                    .map(OrganizationMember::getOrgRole)
                    .orElse(null);
        } else {
            List<OrganizationMember> memberships = organizationMemberRepository.findByUserId(profile.getId());
            if (!memberships.isEmpty()) {
                OrganizationMember member = memberships.get(0);
                orgId = member.getOrganizationId();
                orgRole = member.getOrgRole();
                Organization org = organizationRepository.findById(orgId).orElse(null);
                if (org != null) {
                    orgName = org.getName();
                    orgSubscription = OrgSubscription.fromOrgPlan(org.getPlan()).name();
                }
            }
        }

        return ProfileMeResponse.from(
                profile, gamificationProperties, orgId, orgName, orgSubscription, orgRole);
    }
}
