package com.histar.be.billing.dto;

import java.time.Instant;

public record AdminRecentB2cPaymentItem(
        String orderCode, String status, String userEmail, Integer amount, Instant createdAt) {}
