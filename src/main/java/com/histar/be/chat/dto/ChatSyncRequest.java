package com.histar.be.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChatSyncRequest(
        @NotNull UUID characterId,
        UUID conversationId,
        @NotBlank String userMessage,
        @NotBlank String assistantReply) {}
