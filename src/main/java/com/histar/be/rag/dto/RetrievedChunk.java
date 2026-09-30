package com.histar.be.rag.dto;

import java.util.UUID;

/** One chunk returned by the cosine search, with its source metadata. {@code distance} is cosine distance. */
public record RetrievedChunk(
        UUID id,
        UUID sourceId,
        String content,
        String stationCode,
        String era,
        int chunkIndex,
        String sourceTitle,
        String license,
        double distance) {}
