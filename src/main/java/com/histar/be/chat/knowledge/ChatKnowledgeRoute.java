package com.histar.be.chat.knowledge;

import java.util.Objects;
import java.util.Set;

public record ChatKnowledgeRoute(
        ChatKnowledgeIntent intent,
        Set<KnowledgeScope> scopes,
        String featureKey,
        ChatPageContext pageContext,
        boolean requiresClarification,
        String clarificationReason) {

    public ChatKnowledgeRoute {
        Objects.requireNonNull(intent, "intent");
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    public enum KnowledgeScope {
        PLATFORM,
        HERITAGE,
        USER_STATE
    }
}
