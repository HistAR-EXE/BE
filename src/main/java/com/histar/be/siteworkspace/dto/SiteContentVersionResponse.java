package com.histar.be.siteworkspace.dto;

import com.histar.be.siteworkspace.entity.ContentLifecycleStatus;
import com.histar.be.siteworkspace.entity.SiteContentKind;
import java.time.Instant;
import java.util.UUID;

public record SiteContentVersionResponse(
        UUID id,
        String siteCode,
        SiteContentKind contentKind,
        String resourceKey,
        UUID locationId,
        UUID stationId,
        int version,
        ContentLifecycleStatus status,
        String snapshotJson,
        long revision,
        UUID createdBy,
        UUID submittedBy,
        UUID reviewedBy,
        UUID publishedBy,
        String reviewNote,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant reviewedAt,
        Instant publishedAt) {}
