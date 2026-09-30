package com.histar.be.billing.dto;

/**
 * @param planType {@code PREMIUM} (default) or {@code JOURNEY_PASS}
 * @param siteCode required for JOURNEY_PASS (e.g. cu-chi)
 */
public record B2cCreatePaymentRequest(String returnToPath, String planType, String siteCode) {
    public B2cCreatePaymentRequest(String returnToPath) {
        this(returnToPath, "PREMIUM", null);
    }
}
