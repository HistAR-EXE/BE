package com.histar.be.siteworkspace.dto;

import com.histar.be.siteworkspace.entity.SiteOnboardingStatus;
import java.time.Instant;
import java.util.UUID;

public record SiteOnboardingResponse(
        UUID id,
        UUID requesterId,
        String siteCode,
        String representativeName,
        String organizationName,
        String representativeEmail,
        String evidenceNote,
        SiteOnboardingStatus status,
        UUID reviewedBy,
        String reviewNote,
        Instant createdAt,
        Instant reviewedAt) {}
