package com.histar.be.analytics.dto;

public record OnlineOfflineConversion(
        long usersWithDiscovery,
        long usersWithCheckin,
        long usersDiscoveryThenCheckin,
        double conversionRatePct) {}
