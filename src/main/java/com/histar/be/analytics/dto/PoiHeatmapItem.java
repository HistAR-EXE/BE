package com.histar.be.analytics.dto;

public record PoiHeatmapItem(
        String unlockKey,
        String name,
        Long visitCount,
        Long unlockCount,
        Double avgDwellMs,
        Boolean dropOffHotspot,
        boolean suppressed) {}
