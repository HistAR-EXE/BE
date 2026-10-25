package com.histar.be.fieldtrip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateFieldTripRequest(
        @NotNull UUID orgId,
        @NotBlank String siteCode,
        @NotBlank String title,
        @NotNull Instant visitStart,
        @NotNull Instant visitEnd,
        @NotNull UUID questId,
        @NotNull UUID campaignId) {}
