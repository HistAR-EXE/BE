package com.histar.be.admin.dto;

import com.histar.be.artifact.entity.Artifact;
import java.time.Instant;
import java.util.UUID;

public record AdminArtifactResponse(
        UUID id,
        UUID locationId,
        String name,
        String imageUrl,
        String description,
        String story,
        String unlockKey,
        String reliability,
        Integer sortOrder,
        String collectionName,
        String rarity,
        String sourceUrl,
        String imageSource,
        String imageLicense,
        Boolean factVerified,
        UUID unlockMinigameId,
        Long rowVersion,
        Instant deletedAt,
        UUID deletedBy,
        String deleteReason) {

    public static AdminArtifactResponse from(Artifact artifact) {
        return new AdminArtifactResponse(
                artifact.getId(),
                artifact.getLocationId(),
                artifact.getName(),
                artifact.getImageUrl(),
                artifact.getDescription(),
                artifact.getStory(),
                artifact.getUnlockKey(),
                artifact.getReliability(),
                artifact.getSortOrder(),
                artifact.getCollectionName(),
                artifact.getRarity(),
                artifact.getSourceUrl(),
                artifact.getImageSource(),
                artifact.getImageLicense(),
                artifact.getFactVerified(),
                artifact.getUnlockMinigameId(),
                artifact.getRowVersion(),
                artifact.getDeletedAt(),
                artifact.getDeletedBy(),
                artifact.getDeleteReason());
    }
}
