package com.histar.be.billing.dto;

public record OrgVolumePreviewResponse(
        String planType,
        int licenseCount,
        long unitPriceVnd,
        long subtotalVnd,
        int discountPercent,
        long discountAmountVnd,
        long totalVnd) {}
