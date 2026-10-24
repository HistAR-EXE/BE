package com.histar.be.stations.dto;

import java.util.List;

public record StationContentSnapshot(
        String stationCode,
        String name,
        Integer sortOrder,
        Double lat,
        Double lng,
        String questStepKey,
        Boolean active,
        List<StationBlockSnapshot> blocks) {}
