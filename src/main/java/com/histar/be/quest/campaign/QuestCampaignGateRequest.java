package com.histar.be.quest.campaign;

import jakarta.validation.constraints.NotNull;

public record QuestCampaignGateRequest(
        @NotNull PilotStatus pilotStatus,
        @NotNull ReleaseDecision releaseDecision,
        String note) {}
