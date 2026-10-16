package com.histar.be.quest.narrative;

import com.fasterxml.jackson.databind.JsonNode;

public record NarrativeCreateDraftRequest(long rowVersion, JsonNode graph, String graphJson, String reason) {
    public NarrativeCreateDraftRequest(String graphJson, String reason) {
        this(0L, null, graphJson, reason);
    }
}
