package com.histar.be.chat.guided;

import java.util.List;
import java.util.UUID;

public record GuidedChatResponse(
        UUID conversationId, GuidedAnswer answer, List<String> suggestions, boolean safetyBlocked) {}
