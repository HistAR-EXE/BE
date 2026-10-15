package com.histar.be.analytics.dto;

public record SessionQualityMetrics(
        Double avgDurationMinutes, Double avgDiscoveriesPerSession, boolean suppressed) {}
