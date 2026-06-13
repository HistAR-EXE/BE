package com.histar.be.photoscene.dto;

import com.histar.be.photoscene.entity.TimeLayer;

public record TimeLayerResponse(Integer era, String imageUrl, String caption) {

    public static TimeLayerResponse from(TimeLayer layer) {
        return new TimeLayerResponse(layer.getEra(), layer.getImageUrl(), layer.getCaption());
    }
}
