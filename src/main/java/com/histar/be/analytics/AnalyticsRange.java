package com.histar.be.analytics;

import com.histar.be.common.exception.BusinessRuleException;

public enum AnalyticsRange {
    D7(7), D30(30), D90(90);

    private final int days;
    AnalyticsRange(int days) { this.days = days; }
    public int days() { return days; }

    public static AnalyticsRange parse(String value) {
        String normalized = value == null || value.isBlank() ? "30D" : value.trim().toUpperCase();
        return switch (normalized) {
            case "7D" -> D7;
            case "30D" -> D30;
            case "90D" -> D90;
            default -> throw new BusinessRuleException("range must be 7D, 30D or 90D");
        };
    }
}
