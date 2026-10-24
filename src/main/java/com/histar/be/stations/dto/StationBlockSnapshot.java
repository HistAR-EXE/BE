package com.histar.be.stations.dto;

import com.histar.be.stations.entity.StationBlockType;

public record StationBlockSnapshot(
        StationBlockType blockType,
        String title,
        String body,
        String mediaUrl,
        int sortOrder,
        String metaJson) {}
