package com.histar.be.leaderboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HonorsStoriesResponse(UUID userId, List<Timeline> timeline, List<Path> paths,
        List<Spotlight> spotlights, Instant updatedAt) {
    public record Timeline(String id, String title, String description, Instant occurredAt,
            String href, String icon) {}

    public record Path(String id, String title, String description, int completed, int total,
            int percent, String href, String actionLabel, boolean complete) {}

    public record Leader(UUID userId, String displayName, String avatarUrl, int count, boolean currentUser) {}

    public record Spotlight(String id, String title, String description, String unit, List<Leader> leaders,
            long totalAchievers, int viewerCount, Integer viewerRank) {}
}
