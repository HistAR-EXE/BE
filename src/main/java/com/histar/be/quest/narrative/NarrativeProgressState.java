package com.histar.be.quest.narrative;
import java.util.*;
public record NarrativeProgressState(int version, Set<String> visitedNodeCodes, Set<String> observationCodes, Set<String> chatNodeCodes, Set<String> evidenceCodes, String selectedOutcome) {
    public NarrativeProgressState(int version, Set<String> visitedNodeCodes, Set<String> evidenceCodes, String selectedOutcome) {
        this(version, visitedNodeCodes, new LinkedHashSet<>(), new LinkedHashSet<>(), evidenceCodes, selectedOutcome);
    }
    public NarrativeProgressState(int version, Set<String> visitedNodeCodes, Set<String> observationCodes, Set<String> evidenceCodes, String selectedOutcome) {
        this(version, visitedNodeCodes, observationCodes, new LinkedHashSet<>(), evidenceCodes, selectedOutcome);
    }
    public static NarrativeProgressState start(int version, String node) {
        return new NarrativeProgressState(version, new LinkedHashSet<>(List.of(node)), new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>(), null);
    }
}
