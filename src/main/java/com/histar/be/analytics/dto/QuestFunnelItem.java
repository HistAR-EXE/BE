package com.histar.be.analytics.dto;

import java.util.UUID;

public record QuestFunnelItem(
        UUID questId,
        String title,
        long started,
        long completed,
        double completionRatePct,
        String completionRule) {}
