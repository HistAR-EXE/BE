package com.histar.be.squad.dto;

import java.time.Instant;
import java.util.UUID;

public record SquadCreatedResponse(
        UUID id, String code, String siteCode, UUID leaderUserId, Instant createdAt, int memberCount) {}
