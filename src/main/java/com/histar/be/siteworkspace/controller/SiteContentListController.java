package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteContentVersionResponse;
import com.histar.be.siteworkspace.entity.ContentLifecycleStatus;
import com.histar.be.siteworkspace.service.SiteContentWorkflowService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-workspaces")
@RequiredArgsConstructor
public class SiteContentListController {

    private final SiteContentWorkflowService siteContentWorkflowService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/{siteCode}/content/versions")
    public ApiResponse<List<SiteContentVersionResponse>> list(
            @PathVariable String siteCode,
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.ok(siteContentWorkflowService.listVersions(currentUserId(), siteCode, parseStatus(status)));
    }

    private ContentLifecycleStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ContentLifecycleStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("status không hợp lệ");
        }
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
