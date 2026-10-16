package com.histar.be.analytics.dto;

import java.time.LocalDate;

public record ActivitySeriesPoint(
        LocalDate bucketStart, Long activeUsers, Long eventCount, boolean suppressed) {}
