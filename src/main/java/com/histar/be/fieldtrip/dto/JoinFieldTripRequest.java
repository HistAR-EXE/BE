package com.histar.be.fieldtrip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record JoinFieldTripRequest(@NotBlank String code, @NotNull UUID groupId) {}
