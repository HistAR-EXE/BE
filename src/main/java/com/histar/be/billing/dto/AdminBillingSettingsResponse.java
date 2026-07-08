package com.histar.be.billing.dto;

import java.time.Instant;

public record AdminBillingSettingsResponse(
        int b2cPremiumPriceVnd,
        int chatFreeDailyLimit,
        int orgVolumeDiscountPercent,
        int orgVolumeDiscountMinLicenses,
        String bankCode,
        String accountNumber,
        String accountName,
        String qrTemplate,
        boolean qrShowInfo,
        Instant updatedAt) {}
