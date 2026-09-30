package com.histar.be.events.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventBatchRequest(@NotEmpty @Valid List<PilotEventItem> events) {

    public record PilotEventItem(
            UUID clientUuid,
            @NotBlank @Size(max = 64) String eventType,
            @Size(max = 32) String stationCode,
            String payload,
            @NotNull Instant occurredAt,
            UUID sessionId) {}
}
