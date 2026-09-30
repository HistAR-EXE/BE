package com.histar.be.referral.dto;

import java.time.Instant;

public record ReferralStatsItem(
        String code,
        String creatorName,
        boolean active,
        long visits,
        long uniqueSessions,
        long uniqueUsers,
        Instant lastVisitAt) {}
