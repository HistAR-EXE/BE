package com.histar.be.analytics.dto;

import java.time.Instant;

public record SessionReplayStep(String poiName, String unlockKey, String eventType, Instant at, String source) {}
