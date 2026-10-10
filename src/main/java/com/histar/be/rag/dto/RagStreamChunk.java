package com.histar.be.rag.dto;

import com.histar.be.chat.dto.ChatSource;
import java.util.List;

/** A citation-safe text prefix emitted while a grounded answer is generated. */
public record RagStreamChunk(String content, List<ChatSource> sources) {}
