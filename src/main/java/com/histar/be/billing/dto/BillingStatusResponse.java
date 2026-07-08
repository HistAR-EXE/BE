package com.histar.be.billing.dto;

public record BillingStatusResponse(
        String tier,
        B2cBillingStatus b2c,
        OrgBillingStatus org,
        int chatUsedToday,
        int chatDailyLimit) {}
