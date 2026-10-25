package com.histar.be.fieldtrip.service;

import com.histar.be.billing.service.BillingService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.lms.service.LmsAssignmentService;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FieldTripAccessService {

    private final LmsAssignmentService lmsAssignmentService;
    private final OrganizationRepository organizationRepository;

    public Profile requirePlanningTeacher(UUID teacherId, UUID orgId) {
        if (isPlatformAdmin()) {
            requireActiveOrganization(orgId);
            return Profile.builder().id(teacherId).orgId(orgId).build();
        }
        Profile teacher = lmsAssignmentService.requirePremiumTeacher(teacherId);
        if (orgId == null || teacher.getOrgId() == null || !orgId.equals(teacher.getOrgId())) {
            throw new BusinessRuleException("Không có quyền truy cập organization này");
        }
        requireActiveOrganization(orgId);
        return teacher;
    }

    public void requireActiveMember(UUID teacherId) {
        if (isPlatformAdmin()) {
            return;
        }
        Profile teacher = lmsAssignmentService.requirePremiumTeacher(teacherId);
        requireActiveOrganization(teacher.getOrgId());
    }

    public void requireCanView(UUID teacherId, FieldTrip trip) {
        if (isPlatformAdmin()) {
            return;
        }
        Profile teacher = lmsAssignmentService.requirePremiumTeacher(teacherId);
        if (teacher.getOrgId() == null || trip.getOrgId() == null || !teacher.getOrgId().equals(trip.getOrgId())) {
            throw new BusinessRuleException("Không có quyền truy cập organization này");
        }
        requireActiveOrganization(teacher.getOrgId());
    }

    public void requireCanEdit(UUID teacherId, FieldTrip trip) {
        if (isPlatformAdmin()) {
            return;
        }
        Profile teacher = lmsAssignmentService.requirePremiumTeacher(teacherId);
        if (teacherId == null || !teacherId.equals(trip.getCreatedBy())) {
            throw new BusinessRuleException("Chỉ người tạo chuyến hoặc quản trị viên mới được sửa");
        }
        if (teacher.getOrgId() == null || trip.getOrgId() == null || !teacher.getOrgId().equals(trip.getOrgId())) {
            throw new BusinessRuleException("Không có quyền truy cập organization này");
        }
        requireActiveOrganization(teacher.getOrgId());
    }

    public boolean isPlatformAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> UserRole.ADMIN.authority().equals(authority.getAuthority()));
    }

    private void requireActiveOrganization(UUID orgId) {
        if (orgId == null) {
            throw new ResourceNotFoundException("Tổ chức không tồn tại");
        }
        Organization organization = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ chức không tồn tại"));
        String status = organization.getStatus();
        boolean active = BillingService.ORG_STATUS_ACTIVE.equalsIgnoreCase(status)
                || BillingService.ORG_STATUS_TRIAL_ACTIVE.equalsIgnoreCase(status);
        if (!active) {
            throw new BusinessRuleException("Tổ chức không còn hoạt động");
        }
    }
}
