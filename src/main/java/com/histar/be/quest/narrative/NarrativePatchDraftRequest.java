package com.histar.be.quest.narrative;

import com.fasterxml.jackson.databind.JsonNode;

public record NarrativePatchDraftRequest(long rowVersion, JsonNode graph, String graphJson, String reason) {
    public NarrativePatchDraftRequest(long rowVersion, String graphJson, String reason) {
        this(rowVersion, null, graphJson, reason);
    }
}
