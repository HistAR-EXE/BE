package com.histar.be.squad.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SquadMeResponse(
        UUID id,
        String code,
        String siteCode,
        UUID leaderUserId,
        Instant createdAt,
        List<SquadMemberStateResponse> members) {}
