package com.histar.be.leaderboard.dto;
import java.time.Instant;
public record HonorsCommunityResponse(String scope, long explorers, long completedChallenges,
        long discoveries, long badges, Instant updatedAt) {}
