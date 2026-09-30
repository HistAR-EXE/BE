package com.histar.be.billing.dto;

import java.time.Instant;

public record B2cPaymentStatusResponse(
        String orderCode,
        String status,
        Instant expiresAt,
        Instant paidAt,
        String returnToPath,
        boolean upgraded,
        Integer amountVnd,
        String transferContent,
        Integer receivedAmountVnd,
        Integer remainingAmountVnd) {}
