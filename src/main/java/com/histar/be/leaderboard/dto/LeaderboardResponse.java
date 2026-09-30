package com.histar.be.leaderboard.dto;

import java.util.List;

public record LeaderboardResponse(
        String scope, String city, List<LeaderboardEntryResponse> entries, boolean viewerRankLocked) {}
