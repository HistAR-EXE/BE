package com.histar.be.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChatMessageRequest(
        @NotNull UUID characterId, @NotBlank String message, UUID conversationId) {}
