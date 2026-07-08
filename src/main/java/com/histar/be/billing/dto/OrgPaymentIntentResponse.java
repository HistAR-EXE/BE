package com.histar.be.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgPaymentIntentResponse(
        String provider,
        String orderCode,
        String transferContent,
        long amountVnd,
        long subtotalVnd,
        int discountPercent,
        long discountAmountVnd,
        int licenseCount,
        String bankCode,
        String accountNumber,
        String accountName,
        String qrUrl,
        Instant expiresAt,
        String status,
        String planType,
        UUID organizationId,
        String orgName) {}
