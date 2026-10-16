package com.histar.be.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record AdminRecentB2cPaymentItem(
        String orderCode,
        String status,
        String payerEmail,
        Integer amountVnd,
        Instant createdAt,
        String transferContent,
        Integer receivedAmount) {

    /** Backward-compatible alias for existing Admin Console clients. */
    @JsonProperty("userEmail")
    public String userEmail() {
        return payerEmail;
    }

    /** Backward-compatible alias for existing Admin Console clients. */
    @JsonProperty("amount")
    public Integer amount() {
        return amountVnd;
    }
}
