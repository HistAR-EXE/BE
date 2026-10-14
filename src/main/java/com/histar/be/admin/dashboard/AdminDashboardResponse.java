package com.histar.be.admin.dashboard;

public record AdminDashboardResponse(
        long activePilots,
        long draftCount,
        long reviewPendingCount,
        long trashCount,
        long contentReportsLast30Days) {}
