package com.histar.be.profile.dto;

import java.time.Instant;
import java.util.List;

/** C3 "Journey Wrapped" payload — aggregate of a user's on-site journey. */
public record JourneySummaryResponse(
        String displayName,
        int level,
        int totalPoints,
        int stationsVisited,
        int totalStations,
        int chaptersCompleted,
        int minigamesPlayed,
        int minigameAvgScore,
        int minigameBestScore,
        String bestMinigameTitle,
        Instant firstCheckinAt,
        Instant lastCheckinAt,
        List<String> visitedStationCodes,
        String headline) {}
