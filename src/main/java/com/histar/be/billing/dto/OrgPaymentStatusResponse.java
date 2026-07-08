package com.histar.be.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgPaymentStatusResponse(
        String orderCode,
        String status,
        Instant expiresAt,
        Instant paidAt,
        String returnToPath,
        boolean activated,
        UUID organizationId,
        String planType,
        String orgName) {}
