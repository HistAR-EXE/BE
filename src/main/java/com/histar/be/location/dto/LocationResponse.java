package com.histar.be.location.dto;

import com.histar.be.location.entity.Location;
import java.time.Instant;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        String description,
        Double latitude,
        Double longitude,
        String city,
        String formattedAddress,
        String googleMapsUrl,
        String coverImage,
        Double rating,
        Double distanceKm,
        Boolean isArAvailable,
        UUID unlockPrerequisiteQuestId,
        String unlockNarrative,
        Boolean isUnlocked,
        Instant createdAt) {

    public static LocationResponse from(Location location) {
        return from(location, null, null);
    }

    public static LocationResponse from(Location location, Double distanceKm) {
        return from(location, distanceKm, null);
    }

    public static LocationResponse from(Location location, Double distanceKm, Boolean isUnlocked) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getDescription(),
                location.getLatitude(),
                location.getLongitude(),
                location.getCity(),
                location.getFormattedAddress(),
                location.getGoogleMapsUrl(),
                location.getCoverImage(),
                location.getRating() != null ? location.getRating() : 0.0,
                distanceKm,
                Boolean.TRUE.equals(location.getIsArAvailable()),
                location.getUnlockPrerequisiteQuestId(),
                location.getUnlockNarrative(),
                isUnlocked,
                location.getCreatedAt());
    }
}
