package com.histar.be.billing.dto;

import java.time.Instant;
import java.util.UUID;

/** Payment receipt and the separately linked access period. accessEndsAt is exclusive. */
public record PaymentHistoryItem(UUID id, String kind, String orderCode, String planType,
        String planName, String siteCode, String organizationName, Long amountVnd,
        Long receivedAmountVnd, Long remainingAmountVnd, String status, String rawStatus,
        String activationStatus, String provider, String transferContent, String providerReferenceCode,
        Instant createdAt, Instant paidAt, Instant expiresAt, Instant updatedAt,
        Instant accessStartsAt, Instant accessEndsAt, String activationMessage) {}
