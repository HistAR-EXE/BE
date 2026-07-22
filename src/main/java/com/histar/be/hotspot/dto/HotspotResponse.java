package com.histar.be.hotspot.dto;

import com.histar.be.hotspot.entity.Hotspot;
import java.util.UUID;

public record HotspotResponse(
        UUID id,
        UUID panoramaId,
        Double yaw,
        Double pitch,
        String type,
        String contentRef,
        String label,
        String markerStyle,
        String title,
        String description,
        String imageUrl,
        String unlockKey) {

    public static HotspotResponse from(Hotspot hotspot) {
        return from(hotspot, null, null, null, null);
    }

    public static HotspotResponse from(
            Hotspot hotspot, String title, String description, String imageUrl, String unlockKey) {
        return new HotspotResponse(
                hotspot.getId(),
                hotspot.getPanoramaId(),
                hotspot.getYaw(),
                hotspot.getPitch(),
                hotspot.getType(),
                hotspot.getContentRef(),
                hotspot.getLabel(),
                hotspot.getMarkerStyle(),
                title,
                description,
                imageUrl,
                unlockKey);
    }
}
