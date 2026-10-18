package com.histar.be.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationAnalyticsResponse(
        UUID organizationId,
        String name,
        String orgType,
        long memberCount,
        double completionRatePct,
        double fullTourRatePct,
        String note,
        Instant lastCrmSyncedAt,
        Integer crmLinkedInquiryCount) {}
