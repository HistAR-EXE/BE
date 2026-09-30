package com.histar.be.squad.sync;

import java.time.Instant;
import java.util.UUID;

public record SquadMemberLiveState(
        UUID userId,
        String stationCode,
        Integer progressPercent,
        String progressLabel,
        Instant updatedAt) {

    public static SquadMemberLiveState empty(UUID userId) {
        return new SquadMemberLiveState(userId, null, null, null, null);
    }
}
