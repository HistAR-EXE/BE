package com.histar.be.stations.dto;

import java.util.List;
import java.util.UUID;

public record StationResponse(
        UUID id,
        String siteCode,
        String code,
        String name,
        int sortOrder,
        Double lat,
        Double lng,
        String questStepKey,
        boolean active,
        List<StationBlockResponse> blocks) {}
