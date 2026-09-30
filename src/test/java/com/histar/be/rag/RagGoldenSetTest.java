package com.histar.be.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Sanity checks for the golden question set used by the pilot gate (see docs/RAG_PILOT_GATE.md). */
class RagGoldenSetTest {

    private static final List<String> REQUIRED_KEYS = List.of(
            "\"id\"",
            "\"site_code\"",
            "\"station_code\"",
            "\"question\"",
            "\"expect\"",
            "\"must_cite\"",
            "\"status\"");

    private static final List<String> PILOT_SITES =
            List.of("cu-chi", "hoang-thanh-thang-long", "dai-noi-hue");

    @Test
    void goldenFile_hasAtLeastThirtyRowsPerPilotSite() throws Exception {
        List<String> rows;
        try (InputStream in = getClass().getResourceAsStream("/rag-golden-60.jsonl")) {
            assertThat(in).as("rag-golden-60.jsonl on classpath").isNotNull();
            rows = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .filter(l -> !l.isBlank())
                    .toList();
        }
        assertThat(rows).hasSizeGreaterThanOrEqualTo(90);

        Set<String> ids = new HashSet<>();
        Map<String, Integer> perSite = new HashMap<>();
        for (String row : rows) {
            assertThat(row).startsWith("{").endsWith("}");
            REQUIRED_KEYS.forEach(key -> assertThat(row).as(row).contains(key));
            assertThat(row)
                    .satisfiesAnyOf(
                            r -> assertThat(r).contains("\"expect\":\"answer\""),
                            r -> assertThat(r).contains("\"expect\":\"refuse\""));
            String id = row.substring(row.indexOf("\"id\":\"") + 6, row.indexOf('"', row.indexOf("\"id\":\"") + 6));
            assertThat(ids.add(id)).as("duplicate id " + id).isTrue();
            for (String site : PILOT_SITES) {
                if (row.contains("\"site_code\":\"" + site + "\"")) {
                    perSite.merge(site, 1, Integer::sum);
                }
            }
        }
        assertThat(rows).anyMatch(r -> r.contains("\"expect\":\"refuse\""));
        for (String site : PILOT_SITES) {
            assertThat(perSite.getOrDefault(site, 0))
                    .as("golden rows for " + site)
                    .isGreaterThanOrEqualTo(30);
        }
    }
}
