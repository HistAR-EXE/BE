package com.histar.be.fieldtrip.dto;

import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.group.entity.ParticipationMode;
import java.util.List;
import java.util.UUID;

public record FieldTripLiveBoardResponse(UUID tripId, FieldTripStatus status, List<Row> rows) {

    public record Row(
            UUID groupId,
            String groupName,
            ParticipationMode participationMode,
            UUID userId,
            String displayName,
            int headcount,
            int stationsCompleted,
            int points) {}
}
