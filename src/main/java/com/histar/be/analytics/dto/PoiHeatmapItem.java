package com.histar.be.analytics.dto;

public record PoiHeatmapItem(
        String unlockKey,
        String name,
        long visitCount,
        long unlockCount,
        Double avgDwellMs,
        boolean dropOffHotspot) {}
