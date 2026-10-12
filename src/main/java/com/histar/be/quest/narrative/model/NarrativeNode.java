package com.histar.be.quest.narrative.model;

import java.util.List;

/**
 * A graph node may optionally carry authored copy and an in-app next action.
 * The three-field constructor preserves existing Phase 3 investigation graphs.
 */
public record NarrativeNode(
        String code,
        String type,
        List<NarrativeChoice> choices,
        String title,
        String prompt,
        String actionHref,
        Integer minimumEvidence,
        List<String> observationChecklist,
        List<String> sourceIds,
        String factLabel,
        String evidenceKey,
        String linkedMinigameId,
        Integer rewardXp) {
    public NarrativeNode(String code, String type, List<NarrativeChoice> choices,
                         String title, String prompt, String actionHref, Integer minimumEvidence) {
        this(code, type, choices, title, prompt, actionHref, minimumEvidence,
                List.of(), List.of(), null, null, null, null);
    }

    public NarrativeNode(String code, String type, List<NarrativeChoice> choices) {
        this(code, type, choices, null, null, null, null, List.of(), List.of(), null, null, null, null);
    }
}
