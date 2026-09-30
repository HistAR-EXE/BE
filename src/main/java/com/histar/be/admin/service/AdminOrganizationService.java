package com.histar.be.admin.service;

import com.histar.be.admin.dto.AdminOrganizationSummaryResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Transactional(readOnly = true)
    public List<AdminOrganizationSummaryResponse> listAll() {
        return organizationRepository.findAll().stream()
                .sorted(Comparator.comparing(
                                Organization::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Organization::getId))
                .map(this::toSummary)
                .toList();
    }

    private AdminOrganizationSummaryResponse toSummary(Organization org) {
        long memberCount = organizationMemberRepository.countByOrganizationId(org.getId());
        String planType = org.getPlanType() != null ? org.getPlanType() : org.getPlan();
        return new AdminOrganizationSummaryResponse(
                org.getId(),
                org.getName(),
                inferOrgType(org.getSlug()),
                planType,
                org.getPlan(),
                memberCount);
    }

    private static String inferOrgType(String slug) {
        if (slug == null) {
            return "museum";
        }
        if (slug.contains("school") || slug.contains("fpt") || slug.contains("uni")) {
            return "school";
        }
        if (slug.contains("tour")) {
            return "tour_company";
        }
        return "museum";
    }
}
