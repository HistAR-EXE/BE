package com.histar.be.stations.dto;

import java.util.List;

public record StationContentSnapshot(
        String stationCode,
        String name,
        int sortOrder,
        Double lat,
        Double lng,
        String questStepKey,
        boolean active,
        List<StationBlockSnapshot> blocks) {}
