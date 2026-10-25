package com.histar.be.fieldtrip.dto;

import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.group.entity.ParticipationMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FieldTripReportResponse(
        UUID tripId,
        String title,
        String siteCode,
        UUID questId,
        UUID campaignId,
        FieldTripStatus status,
        Instant generatedAt,
        List<Group> groups) {

    public record Group(
            UUID groupId,
            String name,
            ParticipationMode participationMode,
            int headcount,
            int stationsCompleted,
            int questCompletions,
            int points) {}
}
