package com.histar.be.analytics.dto;

import java.util.UUID;

public record QuestFunnelItem(
        UUID questId,
        String title,
        Long started,
        Long completed,
        Double completionRatePct,
        String completionRule,
        boolean suppressed) {}
