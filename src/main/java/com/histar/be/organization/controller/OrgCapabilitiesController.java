package com.histar.be.organization.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.organization.dto.OrgCapabilities;
import com.histar.be.organization.service.OrganizationEntitlementService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/org/{orgId}/capabilities")
@RequiredArgsConstructor
public class OrgCapabilitiesController {
    private final OrganizationEntitlementService entitlements;
    private final CurrentUserAccessor currentUser;

    @GetMapping
    public ApiResponse<OrgCapabilities> capabilities(@PathVariable UUID orgId) {
        UUID userId = currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(entitlements.capabilities(userId, orgId));
    }
}
