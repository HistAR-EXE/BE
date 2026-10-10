package com.histar.be.quest.campaign;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record QuestCampaignRequest(@NotNull UUID questId, @NotBlank @Pattern(regexp = "DAILY|WEEKLY|SEASONAL|EVENT") String type,
        @NotBlank @Size(max = 160) String title, @NotNull Instant startsAt, @NotNull Instant endsAt, boolean enabled) {}
