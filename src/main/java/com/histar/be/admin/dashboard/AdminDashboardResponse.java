package com.histar.be.admin.dashboard;

import com.histar.be.billing.dto.MrrCoverage;
import com.histar.be.billing.dto.RevenueTrendPoint;
import java.util.List;

public record AdminDashboardResponse(
        long activePilots,
        long draftCount,
        long reviewPendingCount,
        long trashCount,
        long contentReportsLast30Days,
        long draftPendingReview,
        long openContentReports,
        long currentMonthRevenue,
        Double revenueMomPercent,
        long estimatedMrr,
        long estimatedArr,
        List<RevenueTrendPoint> revenueTrend,
        String comparisonStatus,
        MrrCoverage mrrCoverage) {}
