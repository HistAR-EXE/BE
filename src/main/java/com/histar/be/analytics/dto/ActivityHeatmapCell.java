package com.histar.be.analytics.dto;

/** One cell in the Stitch hourly×weekday density grid (Mon=0 … Sun=6). */
public record ActivityHeatmapCell(
        int dayIndex,
        int slotIndex,
        Long activeUsers,
        Long eventCount,
        double intensity,
        boolean suppressed) {}
