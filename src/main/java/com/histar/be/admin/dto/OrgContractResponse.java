package com.histar.be.admin.dto;

import com.histar.be.organization.entity.OrganizationContract;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrgContractResponse(
        UUID id,
        UUID organizationId,
        String title,
        String contractCode,
        Integer licenseSeats,
        String planType,
        String status,
        LocalDate startsOn,
        LocalDate endsOn,
        String documentUrl,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static OrgContractResponse from(OrganizationContract c) {
        return new OrgContractResponse(
                c.getId(),
                c.getOrganizationId(),
                c.getTitle(),
                c.getContractCode(),
                c.getLicenseSeats(),
                c.getPlanType(),
                c.getStatus(),
                c.getStartsOn(),
                c.getEndsOn(),
                c.getDocumentUrl(),
                c.getNotes(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
