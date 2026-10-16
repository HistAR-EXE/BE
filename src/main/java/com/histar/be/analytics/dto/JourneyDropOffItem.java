package com.histar.be.analytics.dto;

public record JourneyDropOffItem(
        String poiName, String unlockKey, Long sessionCount, Double dropOffPct, boolean suppressed) {}
