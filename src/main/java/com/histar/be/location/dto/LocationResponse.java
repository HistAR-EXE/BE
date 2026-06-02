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
        String coverImage,
        Instant createdAt) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getDescription(),
                location.getLatitude(),
                location.getLongitude(),
                location.getCity(),
                location.getCoverImage(),
                location.getCreatedAt());
    }
}
