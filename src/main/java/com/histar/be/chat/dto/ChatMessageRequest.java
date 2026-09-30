package com.histar.be.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * {@code stationCode} / {@code siteCode} optional; when RAG is enabled they scope retrieval
 * (site first, then station) so ST01 at Hue does not pull Cu Chi chunks.
 */
public record ChatMessageRequest(
        @NotNull UUID characterId,
        @NotBlank String message,
        UUID conversationId,
        @Size(max = 32) String stationCode,
        @Size(max = 64) String siteCode) {

    public ChatMessageRequest(UUID characterId, String message, UUID conversationId) {
        this(characterId, message, conversationId, null, null);
    }

    public ChatMessageRequest(UUID characterId, String message, UUID conversationId, String stationCode) {
        this(characterId, message, conversationId, stationCode, null);
    }
}
