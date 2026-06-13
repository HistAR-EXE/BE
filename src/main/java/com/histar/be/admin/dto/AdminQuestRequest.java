package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminQuestRequest(
        @NotNull UUID locationId,
        @NotBlank String title,
        String description,
        String story,
        Integer pointsReward,
        Integer requiredOrder) {}
