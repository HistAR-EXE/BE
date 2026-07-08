package com.histar.be.billing.dto;

public record OrgPlanInfo(
        String planType,
        String label,
        long priceVnd,
        int maxCcu,
        int maxVerifiedAccounts,
        Integer maxAiQueriesPerMonth) {}
