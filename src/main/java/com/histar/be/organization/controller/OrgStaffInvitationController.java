package com.histar.be.organization.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.organization.dto.*;
import com.histar.be.organization.service.*;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/org/{orgId}") @RequiredArgsConstructor
public class OrgStaffInvitationController {
    private final OrgTeacherProvisionService teachers;
    private final OrgEmailInvitationService invitations;
    private final OrganizationEntitlementService entitlements;
    private final CurrentUserAccessor currentUser;

    @PostMapping("/teachers/provision")
    public ApiResponse<ProvisionStudentResponse> provision(@PathVariable UUID orgId,
            @RequestBody @Valid ProvisionStudentRequest request) {
        return ApiResponse.ok(teachers.provision(orgId, userId(), request));
    }

    @GetMapping("/email-invitations")
    public ApiResponse<List<OrgEmailInvitationResponse>> list(@PathVariable UUID orgId) {
        entitlements.requireCapability(userId(), orgId, "manageMembers");
        return ApiResponse.ok(invitations.list(orgId));
    }

    private UUID userId() { return currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized")); }
}
