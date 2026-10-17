package com.histar.be.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminPaymentOperationResponse(
        UUID id,
        String channel,
        String orderCode,
        String status,
        Instant paidAt,
        Instant updatedAt) {}
