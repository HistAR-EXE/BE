package com.histar.be.fieldtrip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record UpdateFieldTripRequest(
        @NotBlank String title,
        @NotBlank String siteCode,
        @NotNull Instant visitStart,
        @NotNull Instant visitEnd,
        @NotNull UUID questId,
        @NotNull UUID campaignId,
        @NotNull Long rowVersion) {}
