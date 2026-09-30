package com.histar.be.stations.dto;

import com.histar.be.stations.entity.StationBlockType;
import java.util.UUID;

public record StationBlockResponse(
        UUID id,
        StationBlockType blockType,
        String title,
        String body,
        String mediaUrl,
        int sortOrder,
        String metaJson) {}
