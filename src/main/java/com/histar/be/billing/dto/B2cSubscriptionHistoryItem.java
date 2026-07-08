package com.histar.be.billing.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record B2cSubscriptionHistoryItem(
        UUID id,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActive,
        int priceVnd,
        String paymentMethod,
        Instant createdAt) {}
