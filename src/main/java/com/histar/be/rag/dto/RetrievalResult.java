package com.histar.be.rag.dto;

import java.util.List;

/** Search output: the chunks plus the pgvector literal of the question (reused by the semantic cache). */
public record RetrievalResult(List<RetrievedChunk> chunks, String vectorLiteral) {

    public static RetrievalResult empty() {
        return new RetrievalResult(List.of(), null);
    }
}
