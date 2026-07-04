package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.profile.entity.UserRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrgAccessService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final CurrentUserAccessor currentUserAccessor;

    public void requireOrgAccess(UUID orgId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        if (isPlatformAdmin() || isOrgTeacher(userId, orgId)) {
            return;
        }
        throw new BusinessRuleException("Không có quyền truy cập organization này");
    }

    public boolean isPlatformAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> UserRole.ADMIN.authority().equals(a.getAuthority()));
    }

    public boolean isOrgTeacher(UUID userId, UUID orgId) {
        return organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, userId)
                .map(m -> "teacher".equalsIgnoreCase(m.getOrgRole()))
                .orElse(false);
    }
}
