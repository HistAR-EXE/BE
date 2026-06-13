package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminArtifactRequest(
        @NotNull UUID locationId,
        @NotBlank String name,
        String imageUrl,
        String description,
        @NotBlank String unlockKey,
        String reliability,
        Integer sortOrder) {}
