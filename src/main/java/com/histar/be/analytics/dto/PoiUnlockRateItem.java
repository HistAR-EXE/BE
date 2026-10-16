package com.histar.be.analytics.dto;

public record PoiUnlockRateItem(
        String unlockKey, String name, Long unlockCount, Double unlockRatePct, boolean suppressed) {}
