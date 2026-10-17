package com.histar.be.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrgCrmSyncResponse(
        UUID organizationId,
        int linkedCount,
        Instant syncedAt,
        List<LinkedInquiry> inquiries) {

    public record LinkedInquiry(UUID id, String siteName, String contactEmail, String status) {}
}
