package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.dto.OrgCapabilities;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authoritative organization-derived rights; never changes a user's purchased tier. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationEntitlementService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;
    private final ProfileRepository profiles;

    public static boolean isActive(Organization org) {
        if (org == null || !("ACTIVE".equalsIgnoreCase(org.getStatus()) || "TRIAL_ACTIVE".equalsIgnoreCase(org.getStatus()))) {
            return false;
        }
        LocalDate today = LocalDate.now(VN_ZONE);
        return (org.getPlanStartDate() == null || !org.getPlanStartDate().isAfter(today))
                && (org.getPlanEndDate() == null || !org.getPlanEndDate().isBefore(today));
    }

    public boolean hasActiveMembership(Profile profile) {
        if (profile == null || profile.getOrgId() == null || !Boolean.TRUE.equals(profile.getEmailVerified())) return false;
        Organization org = organizations.findById(profile.getOrgId()).orElse(null);
        return isActive(org) && plan(org) != OrgSubscription.NONE
                && members.findByOrganizationIdAndUserId(profile.getOrgId(), profile.getId())
                        .filter(member -> member.getAcceptedAt() != null).isPresent();
    }

    public Organization requireActiveMembership(Profile profile) {
        if (!hasActiveMembership(profile)) throw new BusinessRuleException("Thành viên hoặc gói tổ chức không còn hiệu lực");
        return organizations.findById(profile.getOrgId()).orElseThrow();
    }

    public OrgCapabilities capabilities(UUID userId, UUID orgId) {
        Profile profile = profiles.findById(userId).orElseThrow(() -> new BusinessRuleException("Tài khoản không tồn tại"));
        if (orgId == null || !orgId.equals(profile.getOrgId())) throw new BusinessRuleException("Không có quyền truy cập tổ chức này");
        var member = members.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new BusinessRuleException("Bạn không thuộc tổ chức này"));
        Organization org = organizations.findById(orgId).orElseThrow(() -> new BusinessRuleException("Tổ chức không tồn tại"));
        boolean active = isActive(org);
        OrgSubscription plan = plan(org);
        boolean eligible = active && plan != OrgSubscription.NONE && Boolean.TRUE.equals(profile.getEmailVerified()) && member.getAcceptedAt() != null;
        boolean owner = "owner".equalsIgnoreCase(member.getOrgRole());
        boolean manager = eligible && (owner || "teacher".equalsIgnoreCase(member.getOrgRole()));
        boolean standard = plan == OrgSubscription.STANDARD || plan == OrgSubscription.PREMIUM;
        boolean premium = plan == OrgSubscription.PREMIUM;
        return new OrgCapabilities(manager, manager && owner, manager && standard, manager && standard,
                manager && standard, eligible && standard, manager && premium, manager && premium, active, eligible);
    }

    public void requireCapability(UUID userId, UUID orgId, String capability) {
        OrgCapabilities rights = capabilities(userId, orgId);
        boolean allowed = switch (capability) {
            case "manageMembers" -> rights.manageMembers();
            case "manageTeachers" -> rights.manageTeachers();
            case "assignments" -> rights.assignments();
            case "liveBoard" -> rights.liveBoard();
            case "reports" -> rights.reports();
            case "groups" -> rights.groups();
            case "fieldTrips" -> rights.fieldTrips();
            case "contentDrafts" -> rights.contentDrafts();
            default -> false;
        };
        if (!allowed) throw new BusinessRuleException("Gói hoặc vai trò hiện tại không cho phép chức năng này: " + capability);
    }

    private static OrgSubscription plan(Organization org) {
        return org == null ? OrgSubscription.NONE : OrgSubscription.fromPlanType(
                org.getPlanType() != null ? org.getPlanType() : org.getPlan());
    }
}
