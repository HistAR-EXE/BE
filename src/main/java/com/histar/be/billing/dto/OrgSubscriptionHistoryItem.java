package com.histar.be.billing.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrgSubscriptionHistoryItem(
        UUID id,
        String planType,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActive,
        long priceVnd,
        String paymentMethod,
        Instant createdAt) {}
