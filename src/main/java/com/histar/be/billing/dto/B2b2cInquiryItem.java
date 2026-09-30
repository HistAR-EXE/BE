package com.histar.be.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record B2b2cInquiryItem(
        UUID id,
        String siteName,
        String contactName,
        String contactEmail,
        String contactPhone,
        String packageType,
        String message,
        String status,
        String adminNotes,
        Instant contactedAt,
        Instant createdAt,
        String interestSiteCode) {}
