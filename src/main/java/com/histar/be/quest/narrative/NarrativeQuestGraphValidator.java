package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.model.NarrativeChoice;
import com.histar.be.quest.narrative.model.NarrativeNode;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NarrativeQuestGraphValidator {
    private static final Set<String> SUPPORTED_NODE_TYPES = Set.of(
            "OBSERVATION", "MAP_TIMELINE", "SOURCE_COMPARISON", "AI_INTERVIEW",
            "TOUR_360_OBSERVATION", "EVIDENCE", "OUTCOME");

    public List<String> validate(NarrativeQuestGraph graph) {
        List<String> errors = new ArrayList<>();
        List<NarrativeNode> nodes = graph == null || graph.nodes() == null ? List.of() : graph.nodes();
        Set<String> codes = new HashSet<>();
        Set<String> sourceIds = new HashSet<>();
        if (graph != null && graph.sources() != null) {
            graph.sources().forEach(source -> {
                if (source != null && source.sourceId() != null && !source.sourceId().isBlank()) sourceIds.add(source.sourceId());
            });
        }
        for (NarrativeNode node : nodes) {
            if (node.code() == null || node.code().isBlank() || !codes.add(node.code())) {
                errors.add("Node codes must be unique and non-blank");
                continue;
            }
            if (!SUPPORTED_NODE_TYPES.contains(node.type())) {
                errors.add("Node " + node.code() + " has unsupported type " + node.type());
            }
            if (node.actionHref() != null && (!node.actionHref().startsWith("/") || node.actionHref().startsWith("//"))) {
                errors.add("Node " + node.code() + " actionHref must be an in-app path");
            }
            if ("OUTCOME".equals(node.type()) && node.minimumEvidence() != null
                    && (node.minimumEvidence() < 0 || node.minimumEvidence() > (graph == null || graph.evidenceNodeCodes() == null
                    ? 0 : graph.evidenceNodeCodes().size()))) {
                errors.add("Outcome node " + node.code() + " requires more evidence than the graph provides");
            }
            if (node.rewardXp() != null && node.rewardXp() < 0) {
                errors.add("Node " + node.code() + " rewardXp cannot be negative");
            }
            for (String sourceId : node.sourceIds() == null ? List.<String>of() : node.sourceIds()) {
                if (!sourceIds.contains(sourceId)) errors.add("Node " + node.code() + " references missing source " + sourceId);
            }
        }
        if (graph == null || graph.startNodeCode() == null || !codes.contains(graph.startNodeCode())) {
            errors.add("Start node must exist");
        }
        for (NarrativeNode node : nodes) {
            for (NarrativeChoice choice : node.choices() == null ? List.<NarrativeChoice>of() : node.choices()) {
                if (choice.targetNodeCode() == null || !codes.contains(choice.targetNodeCode())) {
                    errors.add("Choice " + choice.code() + " targets missing node " + choice.targetNodeCode());
                }
            }
        }
        requireNodes("Evidence", "EVIDENCE", graph == null ? List.of() : graph.evidenceNodeCodes(), nodes, codes, errors);
        requireNodes("Outcome", "OUTCOME", graph == null ? List.of() : graph.outcomeNodeCodes(), nodes, codes, errors);
        return List.copyOf(errors);
    }

    private static void requireNodes(String label, String expectedType, List<String> referenced,
                                     List<NarrativeNode> nodes, Set<String> codes, List<String> errors) {
        for (String code : referenced == null ? List.<String>of() : referenced) {
            if (!codes.contains(code)) {
                errors.add(label + " references missing node " + code);
                continue;
            }
            NarrativeNode node = nodes.stream().filter(candidate -> code.equals(candidate.code())).findFirst().orElseThrow();
            if (!expectedType.equals(node.type())) {
                errors.add(label + " node " + code + " must use type " + expectedType);
            }
        }
    }
}
