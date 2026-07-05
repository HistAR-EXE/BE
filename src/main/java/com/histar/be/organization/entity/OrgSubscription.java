package com.histar.be.organization.entity;

public enum OrgSubscription {
    NONE,
    ORG_BASIC,
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
            case "org_basic", "basic" -> ORG_BASIC;
            case "org_pro", "pro" -> ORG_PRO;
            case "trial" -> ORG_BASIC;
            default -> NONE;
        };
    }
}
