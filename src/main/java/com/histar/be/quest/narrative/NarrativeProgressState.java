package com.histar.be.quest.narrative;
import java.util.*;
public record NarrativeProgressState(int version, Set<String> visitedNodeCodes, Set<String> evidenceCodes, String selectedOutcome) { public static NarrativeProgressState start(int version, String node) { return new NarrativeProgressState(version, new LinkedHashSet<>(List.of(node)), new LinkedHashSet<>(), null); } }
