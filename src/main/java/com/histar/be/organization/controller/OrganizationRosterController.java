package com.histar.be.organization.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.organization.dto.OrgMembershipResponse;
import com.histar.be.organization.dto.OrgRosterMemberResponse;
import com.histar.be.organization.dto.OrganizationAnalyticsResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.service.OrganizationAnalyticsService;
import com.histar.be.organization.service.OrgAccessService;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrganizationRosterController {

    private final OrganizationAnalyticsService organizationAnalyticsService;
    private final OrgAccessService orgAccessService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/mine")
    public ApiResponse<List<OrgMembershipResponse>> mine() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        List<OrgMembershipResponse> memberships = organizationMemberRepository.findByUserId(userId).stream()
                .map(member -> {
                    Organization org = organizationRepository
                            .findById(member.getOrganizationId())
                            .orElse(null);
                    String name = org != null ? org.getName() : "Organization";
                    return new OrgMembershipResponse(member.getOrganizationId(), name, member.getOrgRole());
                })
                .toList();
        return ApiResponse.ok(memberships);
    }

    @GetMapping("/{orgId}/analytics")
    public ApiResponse<OrganizationAnalyticsResponse> analytics(@PathVariable UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        return ApiResponse.ok(organizationAnalyticsService.analytics(orgId));
    }

    @GetMapping("/{orgId}/roster")
    public ApiResponse<List<OrgRosterMemberResponse>> roster(@PathVariable UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        return ApiResponse.ok(organizationAnalyticsService.roster(orgId));
    }
}
