package com.histar.be.quest.campaign;

import jakarta.validation.constraints.NotBlank;

public record RollbackCampaignRequest(@NotBlank String reason) {}
