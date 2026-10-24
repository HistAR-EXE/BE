package com.histar.be.fieldtrip.dto;

import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
import java.time.Instant;
import java.util.UUID;

public record SiteFieldTripScheduleResponse(
        UUID id,
        String title,
        Instant visitStart,
        Instant visitEnd,
        FieldTripStatus status,
        SiteConfirmationStatus siteConfirmationStatus,
        long groupCount,
        Long rowVersion) {}
