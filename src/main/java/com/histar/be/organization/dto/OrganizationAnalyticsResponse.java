package com.histar.be.organization.dto;

import java.util.UUID;

public record OrganizationAnalyticsResponse(
        UUID organizationId,
        String name,
        String orgType,
        long memberCount,
        double completionRatePct,
        double fullTourRatePct,
        String note) {}
