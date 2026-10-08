package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.quest.narrative.model.NarrativeChoice;
import com.histar.be.quest.narrative.model.NarrativeNode;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class NarrativeQuestGraphValidatorTest {
    private final NarrativeQuestGraphValidator validator = new NarrativeQuestGraphValidator();

    @Test
    void rejects_a_choice_that_targets_a_missing_node() {
        NarrativeQuestGraph graph = new NarrativeQuestGraph(
                "start",
                List.of(new NarrativeNode("start", "OBSERVATION", List.of(new NarrativeChoice("investigate", "missing")))),
                List.of(),
                List.of());

        assertThat(validator.validate(graph)).contains("Choice investigate targets missing node missing");
    }

    @Test
    void accepts_a_graph_with_reachable_evidence_and_outcome() {
        NarrativeQuestGraph graph = new NarrativeQuestGraph(
                "start",
                List.of(
                        new NarrativeNode("start", "OBSERVATION", List.of(new NarrativeChoice("inspect", "evidence"))),
                        new NarrativeNode("evidence", "EVIDENCE", List.of(new NarrativeChoice("conclude", "outcome"))),
                        new NarrativeNode("outcome", "OUTCOME", List.of())),
                List.of("evidence"),
                List.of("outcome"));

        assertThat(validator.validate(graph)).isEmpty();
    }

    @Test
    void rejects_an_unknown_investigation_task_type() {
        NarrativeQuestGraph graph = new NarrativeQuestGraph(
                "start",
                List.of(new NarrativeNode("start", "TELEPORT", List.of())),
                List.of(),
                List.of());

        assertThat(validator.validate(graph)).contains("Node start has unsupported type TELEPORT");
    }

    @Test
    void requires_evidence_and_outcome_lists_to_reference_their_matching_node_types() {
        NarrativeQuestGraph graph = new NarrativeQuestGraph(
                "start",
                List.of(
                        new NarrativeNode("start", "OBSERVATION", List.of()),
                        new NarrativeNode("not-evidence", "SOURCE_COMPARISON", List.of()),
                        new NarrativeNode("not-outcome", "EVIDENCE", List.of())),
                List.of("not-evidence"),
                List.of("not-outcome"));

        assertThat(validator.validate(graph)).contains(
                "Evidence node not-evidence must use type EVIDENCE",
                "Outcome node not-outcome must use type OUTCOME");
    }

    @Test
    void rejects_an_outcome_with_an_impossible_evidence_threshold_or_external_action() {
        NarrativeQuestGraph graph = new NarrativeQuestGraph(
                "start",
                List.of(
                        new NarrativeNode("start", "OBSERVATION", List.of()),
                        new NarrativeNode("evidence", "EVIDENCE", List.of()),
                        new NarrativeNode("ending", "OUTCOME", List.of(), null, null,
                                "https://untrusted.example", 2),
                        new NarrativeNode("other-ending", "OUTCOME", List.of(), null, null,
                                "//untrusted.example", 1)),
                List.of("evidence"),
                List.of("ending", "other-ending"));

        assertThat(validator.validate(graph)).contains(
                "Outcome node ending requires more evidence than the graph provides",
                "Node ending actionHref must be an in-app path",
                "Node other-ending actionHref must be an in-app path");
    }
}
