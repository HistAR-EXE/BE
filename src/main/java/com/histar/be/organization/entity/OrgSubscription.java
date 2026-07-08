package com.histar.be.organization.entity;

public enum OrgSubscription {
    NONE,
    MICRO,
    STANDARD,
    PREMIUM,
    /** @deprecated use MICRO */
    ORG_BASIC,
    /** @deprecated use STANDARD */
    ORG_PRO;

    public static OrgSubscription fromStored(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return OrgSubscription.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }

    public static OrgSubscription fromOrgPlan(String plan) {
        if (plan == null || plan.isBlank()) {
            return NONE;
        }
        return switch (plan.trim().toLowerCase()) {
            case "org_basic", "basic", "micro" -> MICRO;
            case "org_pro", "pro", "standard" -> STANDARD;
            case "premium" -> PREMIUM;
            case "trial" -> MICRO;
            default -> NONE;
        };
    }

    public static OrgSubscription fromPlanType(String planType) {
        if (planType == null || planType.isBlank()) {
            return NONE;
        }
        return switch (planType.trim().toUpperCase()) {
            case "MICRO" -> MICRO;
            case "STANDARD" -> STANDARD;
            case "PREMIUM" -> PREMIUM;
            default -> fromOrgPlan(planType);
        };
    }
}
