package com.histar.be.analytics.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordAnalyticsEventRequest(
        java.util.UUID locationId,
        java.util.UUID visitSessionId,
        @jakarta.validation.constraints.NotBlank String eventType,
        String eventKey,
        String source,
        String contentType,
        String metadata) {}
