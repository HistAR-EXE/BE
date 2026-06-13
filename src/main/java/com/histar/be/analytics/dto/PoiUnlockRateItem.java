package com.histar.be.analytics.dto;

public record PoiUnlockRateItem(String unlockKey, String name, long unlockCount, double unlockRatePct) {}
