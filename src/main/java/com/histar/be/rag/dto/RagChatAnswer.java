package com.histar.be.rag.dto;

import com.histar.be.chat.dto.ChatSource;
import java.util.List;

/**
 * Result of the RAG chat path. {@code refused} is true for the soft refusal (no usable citation);
 * {@code fromCache} marks a semantic-cache hit.
 */
public record RagChatAnswer(String reply, List<ChatSource> sources, boolean refused, boolean fromCache) {}
