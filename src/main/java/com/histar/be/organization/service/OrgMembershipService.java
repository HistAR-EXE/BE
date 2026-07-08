package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.organization.dto.JoinOrgResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.billing.service.BillingService;
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
        if (BillingService.ORG_STATUS_TRIAL_EXPIRED.equalsIgnoreCase(org.getStatus())
                || BillingService.ORG_STATUS_EXPIRED.equalsIgnoreCase(org.getStatus())) {
            throw new BusinessRuleException("Tổ chức đã hết hạn. Vui lòng nâng cấp để mời thêm học sinh.");
        }

        return enrollUserInOrganization(userId, org.getId());
    }

    @Transactional(readOnly = true)
    public void assertOrgHasSeat(UUID orgId) {
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        assertOrgHasSeatOnOrg(org);
    }

    private void assertOrgHasSeatOnOrg(Organization org) {
        int maxAccounts = org.getMaxVerifiedAccounts() != null ? org.getMaxVerifiedAccounts() : 100;
        long currentMembers = organizationMemberRepository.countByOrganizationId(org.getId());
        if (currentMembers >= maxAccounts) {
            throw new BusinessRuleException(
                    "Tổ chức đã đạt giới hạn " + maxAccounts + " tài khoản. Liên hệ giáo viên để nâng gói.");
        }
    }

    @Transactional
    public JoinOrgResponse enrollUserInOrganization(UUID userId, UUID orgId) {
        Organization org = organizationRepository
                .findByIdForUpdate(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserRole currentRole = UserRole.fromStored(profile.getRole());

        if (profile.getOrgId() != null && !profile.getOrgId().equals(orgId)) {
            throw new BusinessRuleException(
                    "Bạn đã thuộc một tổ chức khác. Hãy rời tổ chức hiện tại trước khi tham gia tổ chức mới.");
        }

        boolean alreadyMember = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, userId)
                .isPresent();
        if (!alreadyMember) {
            assertOrgHasSeatOnOrg(org);
        }

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, userId)
                .orElseGet(() -> OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(userId)
                        .orgRole("student")
                        .invitedAt(Instant.now())
                        .build());
        member.setAcceptedAt(Instant.now());
        if (member.getOrgRole() == null || member.getOrgRole().isBlank()) {
            member.setOrgRole("student");
        }
        organizationMemberRepository.save(member);

        profile.setOrgId(orgId);
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

    @Transactional
    public void removeMember(UUID teacherUserId, UUID targetUserId) {
        Profile teacher = profileRepository
                .findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UUID orgId = teacher.getOrgId();
        if (orgId == null) {
            throw new BusinessRuleException("Bạn chưa thuộc tổ chức nào");
        }
        if (teacherUserId.equals(targetUserId)) {
            throw new BusinessRuleException("Không thể tự xoá chính mình khỏi roster bằng chức năng này");
        }

        OrganizationMember teacherMember = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, teacherUserId)
                .orElseThrow(() -> new BusinessRuleException("Bạn không có quyền quản lý tổ chức này"));
        if (!"teacher".equalsIgnoreCase(teacherMember.getOrgRole())) {
            throw new BusinessRuleException("Chỉ giáo viên mới có thể xoá thành viên");
        }

        OrganizationMember targetMember = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new BusinessRuleException("Thành viên không thuộc tổ chức này"));
        if ("teacher".equalsIgnoreCase(targetMember.getOrgRole())) {
            throw new BusinessRuleException("Không thể xoá tài khoản giáo viên bằng thao tác này");
        }

        organizationMemberRepository.delete(targetMember);
        Profile targetProfile = profileRepository
                .findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        targetProfile.setOrgId(null);
        targetProfile.setOrgSubscription(OrgSubscription.NONE.name());
        if (UserRole.fromStored(targetProfile.getRole()) == UserRole.ORG_MEMBER) {
            targetProfile.setRole(UserRole.USER.name());
        }
        profileRepository.save(targetProfile);
    }
}
