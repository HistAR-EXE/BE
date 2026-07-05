package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.organization.dto.JoinOrgResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgMembershipService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public JoinOrgResponse joinOrg(UUID userId, String inviteCodeRaw) {
        String inviteCode = inviteCodeRaw == null ? "" : inviteCodeRaw.trim();
        if (inviteCode.isBlank()) {
            throw new BusinessRuleException("Mã mời không hợp lệ");
        }
        Organization org = organizationRepository
                .findByInviteCodeIgnoreCase(inviteCode)
                .orElseThrow(() -> new BusinessRuleException("Mã mời không hợp lệ hoặc đã hết hạn"));
        if (org.getInviteCodeExpiresAt() != null && org.getInviteCodeExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Mã mời không hợp lệ hoặc đã hết hạn");
        }

        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserRole currentRole = UserRole.fromStored(profile.getRole());

        if (profile.getOrgId() != null && !profile.getOrgId().equals(org.getId())) {
            throw new BusinessRuleException(
                    "Bạn đã thuộc một tổ chức khác. Hãy rời tổ chức hiện tại trước khi tham gia tổ chức mới.");
        }

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(org.getId(), userId)
                .orElseGet(() -> OrganizationMember.builder()
                        .organizationId(org.getId())
                        .userId(userId)
                        .orgRole("student")
                        .invitedAt(Instant.now())
                        .build());
        member.setAcceptedAt(Instant.now());
        if (member.getOrgRole() == null || member.getOrgRole().isBlank()) {
            member.setOrgRole("student");
        }
        organizationMemberRepository.save(member);

        profile.setOrgId(org.getId());
        profile.setOrgSubscription(OrgSubscription.fromOrgPlan(org.getPlan()).name());
        if (currentRole == UserRole.USER) {
            profile.setRole(UserRole.ORG_MEMBER.name());
        }
        profileRepository.save(profile);

        return new JoinOrgResponse(
                org.getId(),
                org.getName(),
                member.getOrgRole(),
                UserRole.fromStored(profile.getRole()).name());
    }

    @Transactional
    public void leaveOrg(UUID userId, boolean confirm) {
        if (!confirm) {
            throw new BusinessRuleException("Cần xác nhận để rời tổ chức");
        }
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UUID orgId = profile.getOrgId();
        if (orgId == null) {
            throw new BusinessRuleException("Bạn chưa thuộc tổ chức nào");
        }
        organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId).ifPresent(organizationMemberRepository::delete);
        profile.setOrgId(null);
        profile.setOrgSubscription(OrgSubscription.NONE.name());
        if (UserRole.fromStored(profile.getRole()) == UserRole.ORG_MEMBER) {
            profile.setRole(UserRole.USER.name());
        }
        profileRepository.save(profile);
    }
}
