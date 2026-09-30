package com.histar.be.billing.dto;

import java.util.List;

public record BillingPublicPricingResponse(
        int b2cPremiumPriceVnd,
        int b2cJourneyPassPriceVnd,
        int chatFreeDailyLimit,
        List<OrgPlanInfo> orgPlans) {}
