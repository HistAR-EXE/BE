package com.histar.be.chat.service;

import java.util.function.Consumer;

public interface ChatLlmClient {

    String name();

    boolean isAvailable();

    String generate(String prompt);

    /**
     * Emits provider text as it becomes available. Providers without native streaming retain the
     * synchronous compatibility path; production Gemini uses its SSE endpoint.
     */
    default void stream(String prompt, Consumer<String> onDelta) {
        onDelta.accept(generate(prompt));
    }
}
