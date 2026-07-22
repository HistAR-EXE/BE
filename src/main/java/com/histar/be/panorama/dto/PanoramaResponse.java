package com.histar.be.panorama.dto;

import com.histar.be.panorama.entity.Panorama;
import java.util.UUID;

public record PanoramaResponse(
        UUID id,
        UUID locationId,
        String imageUrl,
        String title,
        String areaSlug,
        Integer sortOrder,
        Double defaultYaw,
        Double defaultPitch) {

    public static PanoramaResponse from(Panorama panorama) {
        return new PanoramaResponse(
                panorama.getId(),
                panorama.getLocationId(),
                panorama.getImageUrl(),
                panorama.getTitle(),
                panorama.getAreaSlug(),
                panorama.getSortOrder(),
                panorama.getDefaultYaw(),
                panorama.getDefaultPitch());
    }
}
