package com.histar.be.leaderboard.dto;

import java.util.List;
import java.time.Instant;

public record LeaderboardResponse(
        String scope, String city, List<LeaderboardEntryResponse> entries, boolean viewerRankLocked,
        LeaderboardEntryResponse viewer, List<LeaderboardEntryResponse> nearby,
        long totalParticipants, Instant updatedAt, int displayLimit, Integer cutoffXp, long omittedTiedAtCutoff) {
    public LeaderboardResponse(String scope, String city, List<LeaderboardEntryResponse> entries, boolean viewerRankLocked,
            LeaderboardEntryResponse viewer, List<LeaderboardEntryResponse> nearby, long totalParticipants, Instant updatedAt) {
        this(scope, city, entries, viewerRankLocked, viewer, nearby, totalParticipants, updatedAt, 10, null, 0);
    }
}
