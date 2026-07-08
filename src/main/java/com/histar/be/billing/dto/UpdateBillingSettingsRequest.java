package com.histar.be.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateBillingSettingsRequest(
        @Min(1000) Integer b2cPremiumPriceVnd,
        @Min(5) @Max(10) Integer chatFreeDailyLimit,
        @Min(1) @Max(50) Integer orgVolumeDiscountPercent,
        @Min(2) @Max(20) Integer orgVolumeDiscountMinLicenses) {}
