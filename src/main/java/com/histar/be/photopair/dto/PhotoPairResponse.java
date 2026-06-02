package com.histar.be.photopair.dto;

import com.histar.be.photopair.entity.PhotoPair;
import java.util.UUID;

public record PhotoPairResponse(
        UUID id,
        UUID locationId,
        String historicalImage,
        String currentImage,
        Integer year,
        String caption,
        Integer sortOrder) {

    public static PhotoPairResponse from(PhotoPair pair) {
        return new PhotoPairResponse(
                pair.getId(),
                pair.getLocationId(),
                pair.getHistoricalImage(),
                pair.getCurrentImage(),
                pair.getYear(),
                pair.getCaption(),
                pair.getSortOrder());
    }
}
