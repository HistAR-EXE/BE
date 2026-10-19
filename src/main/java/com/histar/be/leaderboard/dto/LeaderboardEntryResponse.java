package com.histar.be.leaderboard.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(
        Integer rank,
        UUID userId,
        String displayName,
        String avatarUrl,
        int totalPoints,
        boolean currentUser,
        int level,
        int badgeCount) {}
