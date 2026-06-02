package com.histar.be.leaderboard.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(
        int rank,
        UUID userId,
        String displayName,
        String avatarUrl,
        int totalPoints,
        boolean currentUser) {}
