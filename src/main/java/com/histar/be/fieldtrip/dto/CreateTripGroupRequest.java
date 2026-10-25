package com.histar.be.fieldtrip.dto;

import com.histar.be.group.entity.ParticipationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTripGroupRequest(
        @NotBlank @Size(min = 2, max = 255) String name,
        @NotNull ParticipationMode participationMode,
        Integer declaredSize) {}
