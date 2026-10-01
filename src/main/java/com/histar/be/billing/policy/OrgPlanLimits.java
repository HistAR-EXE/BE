package com.histar.be.billing.policy;

import com.histar.be.organization.entity.OrgSubscription;

public final class OrgPlanLimits {

    public record Limits(int maxCcu, int maxVerifiedAccounts, Integer maxAiQueriesPerMonth, long priceVnd) {}

    private OrgPlanLimits() {}

    public static Limits forPlan(OrgSubscription plan) {
        return switch (plan) {
            case MICRO, ORG_BASIC -> new Limits(15, 100, 5_000, 8_000_000L);
            case STANDARD, ORG_PRO -> new Limits(40, 400, 30_000, 15_000_000L);
            case PREMIUM -> new Limits(80, 1_000, null, 25_000_000L);
            case NONE -> new Limits(15, 100, 5_000, 0L);
        };
    }
}
