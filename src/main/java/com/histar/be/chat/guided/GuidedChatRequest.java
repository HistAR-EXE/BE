package com.histar.be.chat.guided;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record GuidedChatRequest(
        @NotNull UUID characterId,
        @NotBlank @Size(max = 4000) String message,
        UUID conversationId,
        @Size(max = 32) String stationCode,
        @Size(max = 64) String siteCode,
        ChatMode mode,
        UUID narrativeQuestId,
        @Size(max = 120) String narrativeNodeCode,
        Integer narrativeVersion) {

    public GuidedChatRequest(UUID characterId, String message, UUID conversationId, String stationCode, String siteCode, ChatMode mode) {
        this(characterId, message, conversationId, stationCode, siteCode, mode, null, null, null);
    }

    public ChatMode resolvedMode() {
        return mode == null ? ChatMode.LIGHT_HINT : mode;
    }
}
