package com.histar.be.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record B2b2cInquiryResponse(UUID id, String status, Instant createdAt) {}
