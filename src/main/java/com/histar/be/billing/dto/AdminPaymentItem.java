package com.histar.be.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminPaymentItem(
        UUID id,
        String channel,
        String orderCode,
        String status,
        String payerEmail,
        long amountVnd,
        String planType,
        String organizationName,
        Instant paidAt,
        Instant createdAt) {}
