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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgMembershipService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProfileRepository profileRepository;
    private final EntityManager entityManager;

    private Profile lockProfile(UUID userId) {
        // Provisioning can save a new UUID-backed profile in this same transaction.
        // Materialize that insert (and legitimate auth changes) before refreshing the row.
        entityManager.flush();
        Profile profile = profileRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Authentication may have loaded this managed entity before the lock waited.
        // Refresh so a concurrent enrollment/leave cannot leave a stale orgId in this transaction.
        entityManager.refresh(profile, LockModeType.PESSIMISTIC_WRITE);
        return profile;
    }

    private Organization lockOrganization(UUID orgId) {
        Organization org = organizationRepository.findByIdForUpdate(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        entityManager.refresh(org, LockModeType.PESSIMISTIC_WRITE);
        return org;
    }

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
        // All membership changes lock the affected profile before the destination organization.
        Profile profile = lockProfile(userId);
        Organization org = lockOrganization(orgId);
        if (!OrganizationEntitlementService.isActive(org)) {
            throw new BusinessRuleException("Tổ chức đã hết hạn hoặc không còn hoạt động");
        }

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
        profile.setOrgSubscription(OrgSubscription.fromPlanType(
                org.getPlanType() != null ? org.getPlanType() : org.getPlan()).name());
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
        Profile profile = lockProfile(userId);
        UUID orgId = profile.getOrgId();
        if (orgId == null) {
            throw new BusinessRuleException("Bạn chưa thuộc tổ chức nào");
        }
        lockOrganization(orgId);
        organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId).ifPresent(member -> {
            if ("owner".equalsIgnoreCase(member.getOrgRole())) {
                throw new BusinessRuleException("Chủ tổ chức cần chuyển quyền quản lý trước khi rời tổ chức");
            }
            organizationMemberRepository.delete(member);
        });
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

        Profile targetProfile = lockProfile(targetUserId);
        lockOrganization(orgId);
        if (!orgId.equals(targetProfile.getOrgId())) {
            throw new BusinessRuleException("Thành viên không thuộc tổ chức này");
        }

        OrganizationMember teacherMember = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, teacherUserId)
                .orElseThrow(() -> new BusinessRuleException("Bạn không có quyền quản lý tổ chức này"));
        if (!"teacher".equalsIgnoreCase(teacherMember.getOrgRole()) && !"owner".equalsIgnoreCase(teacherMember.getOrgRole())) {
            throw new BusinessRuleException("Chỉ giáo viên mới có thể xoá thành viên");
        }

        OrganizationMember targetMember = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new BusinessRuleException("Thành viên không thuộc tổ chức này"));
        if ("teacher".equalsIgnoreCase(targetMember.getOrgRole()) || "owner".equalsIgnoreCase(targetMember.getOrgRole())) {
            throw new BusinessRuleException("Không thể xoá tài khoản giáo viên bằng thao tác này");
        }

        organizationMemberRepository.delete(targetMember);
        targetProfile.setOrgId(null);
        targetProfile.setOrgSubscription(OrgSubscription.NONE.name());
        if (UserRole.fromStored(targetProfile.getRole()) == UserRole.ORG_MEMBER) {
            targetProfile.setRole(UserRole.USER.name());
        }
        profileRepository.save(targetProfile);
    }
}
