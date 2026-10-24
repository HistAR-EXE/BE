package com.histar.be.siteworkspace.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMemberResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMembershipResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceSettingsResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceSummaryResponse;
import com.histar.be.siteworkspace.dto.UpdateSiteMemberRequest;
import com.histar.be.siteworkspace.dto.UpdateSiteWorkspaceSettingsRequest;
import com.histar.be.siteworkspace.entity.SiteMembership;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.entity.SiteWorkspaceSettings;
import com.histar.be.siteworkspace.repository.SiteMembershipRepository;
import com.histar.be.siteworkspace.repository.SiteWorkspaceSettingsRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteWorkspaceAccessService {

    private final SiteMembershipRepository siteMembershipRepository;
    private final SiteWorkspaceSettingsRepository siteWorkspaceSettingsRepository;
    private final LocationRepository locationRepository;

    public List<SiteWorkspaceMembershipResponse> listMine(UUID userId) {
        return siteMembershipRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(membership -> {
                    String normalizedSiteCode = normalizeSiteCode(membership.getSiteCode());
                    Location location = locationRepository.findBySiteCodeIgnoreCase(normalizedSiteCode).orElse(null);
                    SiteWorkspaceSettings settings = getOrCreateSettings(normalizedSiteCode);
                    return new SiteWorkspaceMembershipResponse(
                            normalizedSiteCode,
                            location != null ? location.getName() : null,
                            membership.getRole(),
                            settings.isRequiresTripConfirmation());
                })
                .toList();
    }

    public void requireAny(UUID userId, String siteCode, Set<SiteRole> roles) {
        String normalizedSiteCode = resolveSite(siteCode).getSiteCode();
        if (isPlatformAdmin()) {
            return;
        }
        SiteMembership membership = siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(normalizedSiteCode, userId)
                .filter(SiteMembership::isActive)
                .orElseThrow(() -> new BusinessRuleException("Không có quyền truy cập site workspace này"));
        if (!roles.contains(membership.getRole())) {
            throw new BusinessRuleException("Không có quyền truy cập site workspace này");
        }
    }

    public SiteWorkspaceSummaryResponse getWorkspace(UUID userId, String siteCode) {
        Location location = resolveSite(siteCode);
        String normalizedSiteCode = location.getSiteCode();
        SiteRole callerRole = null;
        if (!isPlatformAdmin()) {
            callerRole = siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(normalizedSiteCode, userId)
                    .filter(SiteMembership::isActive)
                    .map(SiteMembership::getRole)
                    .orElseThrow(() -> new BusinessRuleException("Không có quyền truy cập site workspace này"));
        }
        SiteWorkspaceSettings settings = getOrCreateSettings(normalizedSiteCode);
        List<SiteWorkspaceMemberResponse> members = canManage(userId, normalizedSiteCode)
                ? listActiveMembers(normalizedSiteCode)
                : null;
        return new SiteWorkspaceSummaryResponse(
                normalizedSiteCode,
                location.getName(),
                callerRole,
                settings.isRequiresTripConfirmation(),
                members);
    }

    public List<SiteWorkspaceMemberResponse> listMembers(UUID userId, String siteCode) {
        String normalizedSiteCode = resolveSite(siteCode).getSiteCode();
        requireManagerOrAdmin(userId, normalizedSiteCode);
        return listActiveMembers(normalizedSiteCode);
    }

    @Transactional
    public SiteWorkspaceMemberResponse updateMember(UUID actorId, String siteCode, UpdateSiteMemberRequest request) {
        String normalizedSiteCode = resolveSite(siteCode).getSiteCode();
        authorizeRoleMutation(actorId, normalizedSiteCode, request.role());
        SiteMembership membership = siteMembershipRepository
                .findBySiteCodeIgnoreCaseAndUserId(normalizedSiteCode, request.userId())
                .orElseGet(() -> SiteMembership.builder()
                        .siteCode(normalizedSiteCode)
                        .userId(request.userId())
                        .build());
        membership.setSiteCode(normalizedSiteCode);
        membership.setUserId(request.userId());
        membership.setRole(request.role());
        membership.setActive(true);
        membership.setGrantedBy(actorId);
        SiteMembership saved = siteMembershipRepository.save(membership);
        return toMemberResponse(saved);
    }

    @Transactional
    public void revokeMember(UUID actorId, String siteCode, UUID targetUserId) {
        String normalizedSiteCode = resolveSite(siteCode).getSiteCode();
        SiteMembership membership = siteMembershipRepository
                .findBySiteCodeIgnoreCaseAndUserId(normalizedSiteCode, targetUserId)
                .filter(SiteMembership::isActive)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy thành viên site workspace"));
        authorizeRoleMutation(actorId, normalizedSiteCode, membership.getRole());
        membership.setActive(false);
        membership.setGrantedBy(actorId);
        siteMembershipRepository.save(membership);
    }

    @Transactional
    public SiteWorkspaceSettingsResponse updateSettings(
            UUID actorId, String siteCode, UpdateSiteWorkspaceSettingsRequest request) {
        String normalizedSiteCode = resolveSite(siteCode).getSiteCode();
        requireManagerOrAdmin(actorId, normalizedSiteCode);
        SiteWorkspaceSettings settings = getOrCreateSettings(normalizedSiteCode);
        settings.setRequiresTripConfirmation(Boolean.TRUE.equals(request.requiresTripConfirmation()));
        settings.setUpdatedBy(actorId);
        SiteWorkspaceSettings saved = siteWorkspaceSettingsRepository.save(settings);
        return new SiteWorkspaceSettingsResponse(saved.getSiteCode(), saved.isRequiresTripConfirmation());
    }

    public boolean isPlatformAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> UserRole.ADMIN.authority().equals(a.getAuthority()));
    }

    private void requireManagerOrAdmin(UUID userId, String siteCode) {
        if (isPlatformAdmin()) {
            return;
        }
        requireAny(userId, siteCode, Set.of(SiteRole.SITE_MANAGER));
    }

    private void authorizeRoleMutation(UUID actorId, String siteCode, SiteRole targetRole) {
        if (targetRole == SiteRole.SITE_MANAGER) {
            if (!isPlatformAdmin()) {
                throw new BusinessRuleException("Chỉ platform admin mới có thể cấp hoặc thu hồi SITE_MANAGER");
            }
            return;
        }
        requireManagerOrAdmin(actorId, siteCode);
    }

    private boolean canManage(UUID userId, String siteCode) {
        if (isPlatformAdmin()) {
            return true;
        }
        return siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(siteCode, userId)
                .filter(SiteMembership::isActive)
                .map(SiteMembership::getRole)
                .filter(role -> role == SiteRole.SITE_MANAGER)
                .isPresent();
    }

    private List<SiteWorkspaceMemberResponse> listActiveMembers(String siteCode) {
        return siteMembershipRepository.findBySiteCodeIgnoreCaseAndActiveTrue(siteCode).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    private SiteWorkspaceMemberResponse toMemberResponse(SiteMembership membership) {
        return new SiteWorkspaceMemberResponse(membership.getUserId(), membership.getRole(), membership.isActive());
    }

    private Location resolveSite(String siteCode) {
        String normalizedSiteCode = normalizeSiteCode(siteCode);
        return locationRepository.findBySiteCodeIgnoreCase(normalizedSiteCode)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy site workspace"));
    }

    private SiteWorkspaceSettings getOrCreateSettings(String siteCode) {
        String normalizedSiteCode = normalizeSiteCode(siteCode);
        return siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(normalizedSiteCode)
                .orElseGet(() -> siteWorkspaceSettingsRepository.save(SiteWorkspaceSettings.builder()
                        .siteCode(normalizedSiteCode)
                        .requiresTripConfirmation(false)
                        .build()));
    }

    private String normalizeSiteCode(String siteCode) {
        if (siteCode == null || siteCode.isBlank()) {
            throw new BusinessRuleException("Không tìm thấy site workspace");
        }
        return siteCode.trim().toLowerCase();
    }
}
