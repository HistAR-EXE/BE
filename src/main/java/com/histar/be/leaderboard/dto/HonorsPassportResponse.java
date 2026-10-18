package com.histar.be.leaderboard.dto;
import java.time.Instant;
import java.util.*;
public record HonorsPassportResponse(UUID userId, String displayName, int totalPoints, int level,
        String levelName, int badgeCount, List<HeritageBadge> heritageBadges, Instant generatedAt) {
    public record HeritageBadge(String id, Instant earnedAt) {}
}
