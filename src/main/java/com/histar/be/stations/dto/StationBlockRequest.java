package com.histar.be.stations.dto;

import com.histar.be.stations.entity.StationBlockType;
import jakarta.validation.constraints.NotNull;

public record StationBlockRequest(
        @NotNull StationBlockType blockType,
        String title,
        String body,
        String mediaUrl,
        Integer sortOrder,
        String metaJson) {}
