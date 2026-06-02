package com.histar.be.hotspot.dto;

import com.histar.be.hotspot.entity.Hotspot;
import java.util.UUID;

public record HotspotResponse(
        UUID id, UUID panoramaId, Double yaw, Double pitch, String type, String contentRef, String label) {

    public static HotspotResponse from(Hotspot hotspot) {
        return new HotspotResponse(
                hotspot.getId(),
                hotspot.getPanoramaId(),
                hotspot.getYaw(),
                hotspot.getPitch(),
                hotspot.getType(),
                hotspot.getContentRef(),
                hotspot.getLabel());
    }
}
