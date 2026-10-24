package com.histar.be.fieldtrip.dto;

import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
import java.time.Instant;
import java.util.UUID;

public record FieldTripResponse(
        UUID id,
        UUID orgId,
        String siteCode,
        String title,
        Instant visitStart,
        Instant visitEnd,
        UUID questId,
        UUID campaignId,
        String joinCode,
        FieldTripStatus status,
        SiteConfirmationStatus siteConfirmationStatus,
        String siteNote,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        Long rowVersion) {}
