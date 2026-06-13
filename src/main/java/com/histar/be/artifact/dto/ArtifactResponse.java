package com.histar.be.artifact.dto;

import com.histar.be.artifact.entity.Artifact;
import java.util.UUID;

public record ArtifactResponse(
        UUID id,
        UUID locationId,
        String name,
        String imageUrl,
        String description,
        String unlockKey,
        String reliability,
        Integer sortOrder,
        boolean unlocked) {

    public static ArtifactResponse from(Artifact artifact, boolean unlocked) {
        return new ArtifactResponse(
                artifact.getId(),
                artifact.getLocationId(),
                artifact.getName(),
                artifact.getImageUrl(),
                artifact.getDescription(),
                artifact.getUnlockKey(),
                artifact.getReliability(),
                artifact.getSortOrder(),
                unlocked);
    }
}
