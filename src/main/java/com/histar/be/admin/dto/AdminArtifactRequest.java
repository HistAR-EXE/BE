package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminArtifactRequest(
        @NotNull UUID locationId,
        @NotBlank String name,
        String imageUrl,
        String description,
        String story,
        @NotBlank String unlockKey,
        String reliability,
        Integer sortOrder,
        String collectionName,
        String rarity,
        String sourceUrl,
        String imageSource,
        String imageLicense,
        Boolean factVerified,
        UUID unlockMinigameId,
        Long rowVersion) {}
