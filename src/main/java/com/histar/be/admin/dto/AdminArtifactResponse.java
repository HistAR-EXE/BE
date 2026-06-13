package com.histar.be.admin.dto;

import com.histar.be.artifact.entity.Artifact;
import java.util.UUID;

public record AdminArtifactResponse(
        UUID id,
        UUID locationId,
        String name,
        String imageUrl,
        String description,
        String unlockKey,
        String reliability,
        Integer sortOrder) {

    public static AdminArtifactResponse from(Artifact artifact) {
        return new AdminArtifactResponse(
                artifact.getId(),
                artifact.getLocationId(),
                artifact.getName(),
                artifact.getImageUrl(),
                artifact.getDescription(),
                artifact.getUnlockKey(),
                artifact.getReliability(),
                artifact.getSortOrder());
    }
}
