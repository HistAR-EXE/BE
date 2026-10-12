package com.histar.be.quest.narrative;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record NarrativePilotAdminResponse(
        UUID questId,
        String questTitle,
        UUID investigationId,
        int version,
        boolean enabled,
        String reviewStatus,
        Set<String> approvedLocales,
        String reviewedBy,
        Instant reviewedAt,
        boolean globalFlagEnabled,
        boolean graphValid,
        int sourceCount,
        List<String> readinessErrors) {}
