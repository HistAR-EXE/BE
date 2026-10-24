package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteWorkspaceMembershipResponse;
import com.histar.be.siteworkspace.dto.SiteWorkspaceSummaryResponse;
import com.histar.be.siteworkspace.service.SiteWorkspaceAccessService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-workspaces")
@RequiredArgsConstructor
public class SiteWorkspaceController {

    private final SiteWorkspaceAccessService siteWorkspaceAccessService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/mine")
    public ApiResponse<List<SiteWorkspaceMembershipResponse>> mine() {
        return ApiResponse.ok(siteWorkspaceAccessService.listMine(currentUserId()));
    }

    @GetMapping("/{siteCode}")
    public ApiResponse<SiteWorkspaceSummaryResponse> getWorkspace(@PathVariable String siteCode) {
        return ApiResponse.ok(siteWorkspaceAccessService.getWorkspace(currentUserId(), siteCode));
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
