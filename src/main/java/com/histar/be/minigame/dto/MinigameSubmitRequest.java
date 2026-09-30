package com.histar.be.minigame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MinigameSubmitRequest(@NotNull @Min(0) @Max(100_000) Integer score) {}
