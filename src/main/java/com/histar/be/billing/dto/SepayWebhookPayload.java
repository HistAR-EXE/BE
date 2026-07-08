package com.histar.be.billing.dto;

public record SepayWebhookPayload(
        Long id,
        String gateway,
        String transactionDate,
        String accountNumber,
        String subAccount,
        String code,
        String content,
        String transferType,
        String description,
        Long transferAmount,
        Long accumulated,
        String referenceCode) {}
