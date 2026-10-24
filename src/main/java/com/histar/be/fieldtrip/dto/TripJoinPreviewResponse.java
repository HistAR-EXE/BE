package com.histar.be.fieldtrip.dto;

import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.group.entity.ParticipationMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TripJoinPreviewResponse(
        String title,
        String siteCode,
        Instant visitStart,
        Instant visitEnd,
        FieldTripStatus status,
        List<GroupPreview> groups) {

    public record GroupPreview(
            UUID id,
            String name,
            ParticipationMode participationMode,
            Integer declaredSize,
            int memberCount) {}
}
