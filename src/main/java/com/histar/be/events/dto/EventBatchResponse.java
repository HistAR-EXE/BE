package com.histar.be.events.dto;

/** {@code rejectedUnsupported} counts events whose {@code eventType} is not in the pilot allowlist. */
public record EventBatchResponse(int accepted, int skippedDuplicate, int rejectedUnsupported) {

    public EventBatchResponse(int accepted, int skippedDuplicate) {
        this(accepted, skippedDuplicate, 0);
    }
}