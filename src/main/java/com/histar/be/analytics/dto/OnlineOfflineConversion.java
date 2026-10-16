package com.histar.be.analytics.dto;

public record OnlineOfflineConversion(
        Long usersWithDiscovery,
        Long usersWithCheckin,
        Long usersDiscoveryThenCheckin,
        Double conversionRatePct,
        boolean suppressed) {}
