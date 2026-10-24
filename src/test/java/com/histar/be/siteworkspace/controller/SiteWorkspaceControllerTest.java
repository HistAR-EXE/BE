package com.histar.be.siteworkspace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMemberResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMembershipResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceSettingsResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceSummaryResponse;
import com.histar.be.siteworkspace.dto.UpdateSiteMemberRequest;
import com.histar.be.siteworkspace.dto.UpdateSiteWorkspaceSettingsRequest;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.service.SiteWorkspaceAccessService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteWorkspaceControllerTest {

    @Mock
    private SiteWorkspaceAccessService siteWorkspaceAccessService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private SiteWorkspaceController siteWorkspaceController;

    @InjectMocks
    private SiteWorkspaceMemberController siteWorkspaceMemberController;

    @Test
    void mine_wrapsServiceResponse() {
        UUID userId = UUID.randomUUID();
        List<SiteWorkspaceMembershipResponse> memberships = List.of(
                new SiteWorkspaceMembershipResponse(
                        "imperial-city",
                        "Imperial City",
                        SiteRole.SITE_MANAGER,
                        true));
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(userId));
        when(siteWorkspaceAccessService.listMine(userId)).thenReturn(memberships);

        ApiResponse<List<SiteWorkspaceMembershipResponse>> response = siteWorkspaceController.mine();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(memberships);
    }

    @Test
    void mine_throwsWhenUnauthenticated() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> siteWorkspaceController.mine())
                .isInstanceOf(AuthException.class)
                .hasMessage("Unauthorized");
    }

    @Test
    void getWorkspace_wrapsSummaryResponse() {
        UUID userId = UUID.randomUUID();
        SiteWorkspaceSummaryResponse summary = new SiteWorkspaceSummaryResponse(
                "imperial-city",
                "Imperial City",
                SiteRole.CONTENT_EDITOR,
                false,
                null);
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(userId));
        when(siteWorkspaceAccessService.getWorkspace(userId, "IMPERIAL-CITY")).thenReturn(summary);

        ApiResponse<SiteWorkspaceSummaryResponse> response =
                siteWorkspaceController.getWorkspace("IMPERIAL-CITY");

        assertThat(response.getData()).isEqualTo(summary);
    }

    @Test
    void members_wrapsMemberList() {
        UUID userId = UUID.randomUUID();
        List<SiteWorkspaceMemberResponse> members = List.of(
                new SiteWorkspaceMemberResponse(UUID.randomUUID(), SiteRole.CONTENT_REVIEWER, true));
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(userId));
        when(siteWorkspaceAccessService.listMembers(userId, "imperial-city")).thenReturn(members);

        ApiResponse<List<SiteWorkspaceMemberResponse>> response =
                siteWorkspaceMemberController.members("imperial-city");

        assertThat(response.getData()).isEqualTo(members);
    }

    @Test
    void updateMember_usesAuthenticatedUser() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UpdateSiteMemberRequest request = new UpdateSiteMemberRequest(targetUserId, SiteRole.CONTENT_EDITOR);
        SiteWorkspaceMemberResponse updated =
                new SiteWorkspaceMemberResponse(targetUserId, SiteRole.CONTENT_EDITOR, true);
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(siteWorkspaceAccessService.updateMember(actorId, "imperial-city", request)).thenReturn(updated);

        ApiResponse<SiteWorkspaceMemberResponse> response =
                siteWorkspaceMemberController.updateMember("imperial-city", request);

        assertThat(response.getData()).isEqualTo(updated);
    }

    @Test
    void revokeMember_wrapsVoidResponse() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));

        ApiResponse<Void> response = siteWorkspaceMemberController.revokeMember("imperial-city", targetUserId);

        verify(siteWorkspaceAccessService).revokeMember(actorId, "imperial-city", targetUserId);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
    }

    @Test
    void updateSettings_wrapsSettingsResponse() {
        UUID actorId = UUID.randomUUID();
        UpdateSiteWorkspaceSettingsRequest request = new UpdateSiteWorkspaceSettingsRequest(true);
        SiteWorkspaceSettingsResponse settings = new SiteWorkspaceSettingsResponse("imperial-city", true);
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(siteWorkspaceAccessService.updateSettings(actorId, "imperial-city", request)).thenReturn(settings);

        ApiResponse<SiteWorkspaceSettingsResponse> response =
                siteWorkspaceMemberController.updateSettings("imperial-city", request);

        assertThat(response.getData()).isEqualTo(settings);
    }
}
