package com.histar.be.chat.guided;

import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.service.PreparedChatResponse;
import java.util.List;
import java.util.UUID;

/** Holds a validated answer until the stream has reached its complete event. */
record GuidedChatDraft(
        UUID userId,
        GuidedChatRequest request,
        ChatMessageRequest messageRequest,
        PreparedChatResponse prepared,
        GuidedChatResponse response,
        UUID locationId,
        String lastVisitedPoi,
        String questState) {
    static GuidedChatDraft blocked(GuidedChatResponse response) {
        return new GuidedChatDraft(null, null, null, null, response, null, null, null);
    }
}
