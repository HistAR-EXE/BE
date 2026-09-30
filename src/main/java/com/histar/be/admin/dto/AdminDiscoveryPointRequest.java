package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AdminDiscoveryPointRequest(
        @NotNull UUID locationId,
        @NotBlank String name,
        BigDecimal mapXPct,
        BigDecimal mapYPct,
        @NotBlank String unlockKey,
        Integer sortOrder,
        Double yaw,
        Double pitch) {}
