package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMemberResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceSettingsResponse;
import com.histar.be.siteworkspace.dto.UpdateSiteMemberRequest;
import com.histar.be.siteworkspace.dto.UpdateSiteWorkspaceSettingsRequest;
import com.histar.be.siteworkspace.service.SiteWorkspaceAccessService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-workspaces")
@RequiredArgsConstructor
public class SiteWorkspaceMemberController {

    private final SiteWorkspaceAccessService siteWorkspaceAccessService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/{siteCode}/members")
    public ApiResponse<List<SiteWorkspaceMemberResponse>> members(@PathVariable String siteCode) {
        return ApiResponse.ok(siteWorkspaceAccessService.listMembers(currentUserId(), siteCode));
    }

    @PutMapping("/{siteCode}/members")
    public ApiResponse<SiteWorkspaceMemberResponse> updateMember(
            @PathVariable String siteCode,
            @RequestBody @Valid UpdateSiteMemberRequest request) {
        return ApiResponse.ok(siteWorkspaceAccessService.updateMember(currentUserId(), siteCode, request));
    }

    @DeleteMapping("/{siteCode}/members/{userId}")
    public ApiResponse<Void> revokeMember(@PathVariable String siteCode, @PathVariable UUID userId) {
        siteWorkspaceAccessService.revokeMember(currentUserId(), siteCode, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{siteCode}/settings")
    public ApiResponse<SiteWorkspaceSettingsResponse> updateSettings(
            @PathVariable String siteCode,
            @RequestBody @Valid UpdateSiteWorkspaceSettingsRequest request) {
        return ApiResponse.ok(siteWorkspaceAccessService.updateSettings(currentUserId(), siteCode, request));
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
