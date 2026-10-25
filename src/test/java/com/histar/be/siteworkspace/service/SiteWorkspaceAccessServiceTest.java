package com.histar.be.siteworkspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMemberResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMembershipResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceSummaryResponse;
import com.histar.be.siteworkspace.dto.UpdateSiteMemberRequest;
import com.histar.be.siteworkspace.dto.UpdateSiteWorkspaceSettingsRequest;
import com.histar.be.siteworkspace.entity.SiteMembership;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.entity.SiteWorkspaceSettings;
import com.histar.be.siteworkspace.repository.SiteMembershipRepository;
import com.histar.be.siteworkspace.repository.SiteWorkspaceSettingsRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SiteWorkspaceAccessServiceTest {

    private static final String SITE_CODE = "imperial-city";

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private SiteWorkspaceSettingsRepository siteWorkspaceSettingsRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private SiteWorkspaceAccessService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMine_returnsActiveMembershipsWithSettings() {
        UUID userId = UUID.randomUUID();
        when(siteMembershipRepository.findByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(membership(SITE_CODE, userId, SiteRole.SITE_MANAGER, true)));
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(settings(SITE_CODE, true)));

        List<SiteWorkspaceMembershipResponse> result = service.listMine(userId);

        assertThat(result).containsExactly(new SiteWorkspaceMembershipResponse(
                SITE_CODE,
                "Imperial City",
                SiteRole.SITE_MANAGER,
                true));
    }

    @Test
    void listMine_returnsDefaultConfirmationWithoutPersistingSettings() {
        UUID userId = UUID.randomUUID();
        when(siteMembershipRepository.findByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(membership(SITE_CODE, userId, SiteRole.CONTENT_EDITOR, true)));
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.empty());

        List<SiteWorkspaceMembershipResponse> result = service.listMine(userId);

        assertThat(result).containsExactly(new SiteWorkspaceMembershipResponse(
                SITE_CODE,
                "Imperial City",
                SiteRole.CONTENT_EDITOR,
                false));
        verify(siteWorkspaceSettingsRepository, never()).save(any());
    }

    @Test
    void requireAny_allowsManagerForMatchingRole() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.SITE_MANAGER, true)));

        service.requireAny(userId, SITE_CODE, Set.of(SiteRole.SITE_MANAGER));
    }

    @Test
    void requireAny_rejectsEditorForManagerOnlyAction() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.CONTENT_EDITOR, true)));

        assertThatThrownBy(() -> service.requireAny(userId, SITE_CODE, Set.of(SiteRole.SITE_MANAGER)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không có quyền");
    }

    @Test
    void requireAny_allowsReviewerForReviewerRole() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.CONTENT_REVIEWER, true)));

        service.requireAny(userId, SITE_CODE, Set.of(SiteRole.CONTENT_REVIEWER));
    }

    @Test
    void requireAny_rejectsInactiveMembership() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.SITE_MANAGER, false)));

        assertThatThrownBy(() -> service.requireAny(userId, SITE_CODE, Set.of(SiteRole.SITE_MANAGER)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không có quyền");
    }

    @Test
    void requireAny_rejectsCrossSiteMembership() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireAny(userId, SITE_CODE, Set.of(SiteRole.CONTENT_EDITOR)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không có quyền");
    }

    @Test
    void requireAny_allowsAdminBypassWithoutMembership() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "n/a",
                        List.of(() -> "ROLE_ADMIN")));
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));

        service.requireAny(userId, SITE_CODE, Set.of(SiteRole.SITE_MANAGER));

        verify(siteMembershipRepository, never()).findBySiteCodeIgnoreCaseAndUserId(any(), any());
    }

    @Test
    void getWorkspace_returnsDefaultSettingsWithoutPersisting() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.CONTENT_EDITOR, true)));
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.empty());

        SiteWorkspaceSummaryResponse result = service.getWorkspace(userId, SITE_CODE);

        assertThat(result.siteCode()).isEqualTo(SITE_CODE);
        assertThat(result.locationName()).isEqualTo("Imperial City");
        assertThat(result.callerRole()).isEqualTo(SiteRole.CONTENT_EDITOR);
        assertThat(result.requiresTripConfirmation()).isFalse();
        assertThat(result.members()).isNull();
        verify(siteWorkspaceSettingsRepository, never()).save(any());
    }

    @Test
    void getWorkspace_includesLocationProfile() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder()
                        .siteCode(SITE_CODE)
                        .name("Imperial City")
                        .description("Citadel")
                        .formattedAddress("Hue")
                        .coverImage("https://cdn.example/cover.jpg")
                        .sources("Đại Nam thực lục")
                        .build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.CONTENT_EDITOR, true)));
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.empty());

        SiteWorkspaceSummaryResponse result = service.getWorkspace(userId, SITE_CODE);

        assertThat(result.name()).isEqualTo("Imperial City");
        assertThat(result.description()).isEqualTo("Citadel");
        assertThat(result.formattedAddress()).isEqualTo("Hue");
        assertThat(result.coverImage()).isEqualTo("https://cdn.example/cover.jpg");
        assertThat(result.sources()).isEqualTo("Đại Nam thực lục");
    }

    @Test
    void getWorkspace_includesMembersForManagers() {
        UUID userId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.SITE_MANAGER, true)));
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(settings(SITE_CODE, true)));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndActiveTrue(SITE_CODE))
                .thenReturn(List.of(
                        membership(SITE_CODE, userId, SiteRole.SITE_MANAGER, true),
                        membership(SITE_CODE, memberId, SiteRole.CONTENT_REVIEWER, true)));

        SiteWorkspaceSummaryResponse result = service.getWorkspace(userId, SITE_CODE);

        assertThat(result.members()).containsExactly(
                new SiteWorkspaceMemberResponse(userId, SiteRole.SITE_MANAGER, true),
                new SiteWorkspaceMemberResponse(memberId, SiteRole.CONTENT_REVIEWER, true));
    }

    @Test
    void listMembers_requiresManagerOrAdmin() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, userId))
                .thenReturn(Optional.of(membership(SITE_CODE, userId, SiteRole.CONTENT_EDITOR, true)));

        assertThatThrownBy(() -> service.listMembers(userId, SITE_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không có quyền");
    }

    @Test
    void updateMember_allowsManagerToGrantEditor() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, actorId))
                .thenReturn(Optional.of(membership(SITE_CODE, actorId, SiteRole.SITE_MANAGER, true)));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, targetUserId))
                .thenReturn(Optional.empty());
        when(siteMembershipRepository.save(any(SiteMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SiteWorkspaceMemberResponse result = service.updateMember(
                actorId,
                SITE_CODE,
                new UpdateSiteMemberRequest(targetUserId, SiteRole.CONTENT_EDITOR));

        assertThat(result).isEqualTo(new SiteWorkspaceMemberResponse(targetUserId, SiteRole.CONTENT_EDITOR, true));
    }

    @Test
    void updateMember_rejectsManagerGrantingSiteManager() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));

        assertThatThrownBy(() -> service.updateMember(
                        actorId,
                        SITE_CODE,
                        new UpdateSiteMemberRequest(targetUserId, SiteRole.SITE_MANAGER)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SITE_MANAGER");
    }

    @Test
    void updateMember_rejectsManagerDemotingActiveSiteManager() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, targetUserId))
                .thenReturn(Optional.of(membership(SITE_CODE, targetUserId, SiteRole.SITE_MANAGER, true)));

        assertThatThrownBy(() -> service.updateMember(
                        actorId,
                        SITE_CODE,
                        new UpdateSiteMemberRequest(targetUserId, SiteRole.CONTENT_EDITOR)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SITE_MANAGER");
        verify(siteMembershipRepository, never()).save(any());
    }

    @Test
    void revokeMember_allowsManagerToDeactivateReviewer() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        SiteMembership reviewer = membership(SITE_CODE, targetUserId, SiteRole.CONTENT_REVIEWER, true);
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, actorId))
                .thenReturn(Optional.of(membership(SITE_CODE, actorId, SiteRole.SITE_MANAGER, true)));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, targetUserId))
                .thenReturn(Optional.of(reviewer));
        when(siteMembershipRepository.save(reviewer)).thenReturn(reviewer);

        service.revokeMember(actorId, SITE_CODE, targetUserId);

        assertThat(reviewer.isActive()).isFalse();
    }

    @Test
    void revokeMember_rejectsManagerRevokingSiteManager() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, targetUserId))
                .thenReturn(Optional.of(membership(SITE_CODE, targetUserId, SiteRole.SITE_MANAGER, true)));

        assertThatThrownBy(() -> service.revokeMember(actorId, SITE_CODE, targetUserId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SITE_MANAGER");
    }

    @Test
    void updateSettings_allowsManager() {
        UUID actorId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(Location.builder().siteCode(SITE_CODE).name("Imperial City").build()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, actorId))
                .thenReturn(Optional.of(membership(SITE_CODE, actorId, SiteRole.SITE_MANAGER, true)));
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE_CODE))
                .thenReturn(Optional.of(settings(SITE_CODE, false)));
        when(siteWorkspaceSettingsRepository.save(any(SiteWorkspaceSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSettings(
                actorId,
                SITE_CODE,
                new UpdateSiteWorkspaceSettingsRequest(true));

        assertThat(result.requiresTripConfirmation()).isTrue();
    }

    @Test
    void getWorkspace_throwsWhenSiteUnknown() {
        UUID userId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkspace(userId, SITE_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    private static SiteMembership membership(String siteCode, UUID userId, SiteRole role, boolean active) {
        return SiteMembership.builder()
                .siteCode(siteCode)
                .userId(userId)
                .role(role)
                .active(active)
                .build();
    }

    private static SiteWorkspaceSettings settings(String siteCode, boolean requiresTripConfirmation) {
        return SiteWorkspaceSettings.builder()
                .siteCode(siteCode)
                .requiresTripConfirmation(requiresTripConfirmation)
                .build();
    }
}
