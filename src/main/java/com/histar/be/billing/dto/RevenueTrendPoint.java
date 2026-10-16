package com.histar.be.billing.dto;

public record RevenueTrendPoint(String month, long b2cRevenue, long b2bRevenue, long totalRevenue) {}
