package com.histar.be.billing.dto;

import java.util.List;

public record AdminBillingOverviewResponse(
        long currentMonthRevenue,
        Double revenueMomPercent,
        String comparisonStatus,
        Double revenueYoyPercent,
        String yoyComparisonStatus,
        long estimatedMrr,
        long estimatedArr,
        List<RevenueTrendPoint> revenueTrend,
        MrrCoverage mrrCoverage,
        List<PlanRevenuePoint> planRevenue) {}
