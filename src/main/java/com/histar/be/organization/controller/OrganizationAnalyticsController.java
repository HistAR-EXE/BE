package com.histar.be.organization.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.organization.dto.OrganizationAnalyticsResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class OrganizationAnalyticsController {

    private final OrganizationRepository organizationRepository;

    @GetMapping("/{orgId}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganizationAnalyticsResponse> analytics(@PathVariable UUID orgId) {
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new com.histar.be.common.exception.BusinessRuleException("Organization không tồn tại"));
        return ApiResponse.ok(new OrganizationAnalyticsResponse(
                org.getId(),
                org.getName(),
                inferType(org.getSlug()),
                0L,
                0.0,
                0.0,
                "Pilot B2B2E — gắn organization_members để xem cohort thật."));
    }

    private static String inferType(String slug) {
        if (slug == null) return "museum";
        if (slug.contains("school") || slug.contains("fpt") || slug.contains("uni")) return "school";
        if (slug.contains("tour")) return "tour_company";
        return "museum";
    }
}
