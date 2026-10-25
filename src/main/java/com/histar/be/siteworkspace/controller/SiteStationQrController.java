package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteStationQrResponse;
import com.histar.be.siteworkspace.service.SiteContentWorkflowService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-workspaces/{siteCode}/stations/{stationCode}/qr")
@RequiredArgsConstructor
public class SiteStationQrController {

    private final SiteContentWorkflowService siteContentWorkflowService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<SiteStationQrResponse> issue(
            @PathVariable String siteCode,
            @PathVariable String stationCode,
            @RequestParam(name = "static", defaultValue = "false") boolean staticQr) {
        UUID userId = currentUserId();
        if (!staticQr) {
            throw new BusinessRuleException("Chỉ hỗ trợ QR tĩnh");
        }
        return ApiResponse.ok(siteContentWorkflowService.issueStaticQr(userId, siteCode, stationCode));
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
