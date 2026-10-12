package com.histar.be.quest.narrative.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Versioned authoring contract; persistence keeps this graph immutable once published. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NarrativeQuestGraph(
        String startNodeCode,
        List<NarrativeNode> nodes,
        List<String> evidenceNodeCodes,
        List<String> outcomeNodeCodes,
        List<NarrativeSource> sources) {
    public NarrativeQuestGraph(String startNodeCode, List<NarrativeNode> nodes,
                               List<String> evidenceNodeCodes, List<String> outcomeNodeCodes) {
        this(startNodeCode, nodes, evidenceNodeCodes, outcomeNodeCodes, List.of());
    }
}
