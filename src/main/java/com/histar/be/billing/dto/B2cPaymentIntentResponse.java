package com.histar.be.billing.dto;

import java.time.Instant;

public record B2cPaymentIntentResponse(
        String provider,
        String orderCode,
        String transferContent,
        int amountVnd,
        String bankCode,
        String accountNumber,
        String accountName,
        String qrUrl,
        Instant expiresAt,
        String status) {}
