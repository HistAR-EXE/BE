package com.histar.be.photoframe.dto;

import com.histar.be.photoframe.entity.PhotoFrame;
import java.util.UUID;

public record PhotoFrameResponse(UUID id, String name, String imageUrl, String era, Integer sortOrder) {

    public static PhotoFrameResponse from(PhotoFrame frame) {
        return new PhotoFrameResponse(
                frame.getId(), frame.getName(), frame.getImageUrl(), frame.getEra(), frame.getSortOrder());
    }
}
