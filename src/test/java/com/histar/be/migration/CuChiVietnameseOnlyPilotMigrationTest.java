package com.histar.be.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CuChiVietnameseOnlyPilotMigrationTest {

    private static final Pattern GRAPH_JSON = Pattern.compile("graph_json\\s*=\\s*\\$graph\\$(.*?)\\$graph\\$", Pattern.DOTALL);

    @Test
    void keepsPilotDisabledAndUsesOnlyVietnameseOfficialCitations() throws IOException {
        String sql = migrationSql();
        Matcher matcher = GRAPH_JSON.matcher(sql);
        assertThat(matcher.find()).as("V60 graph_json payload").isTrue();

        JsonNode graph = new ObjectMapper().readTree(matcher.group(1));
        assertThat(sql).contains("enabled = FALSE").contains("review_status = 'DRAFT'");
        assertThat(graph.path("sources")).isNotEmpty();
        for (JsonNode source : graph.path("sources")) {
            assertThat(source.path("locale").asText()).isEqualTo("vi");
            assertThat(source.path("sourceId").asText()).isNotBlank();
            assertThat(source.path("version").asText()).isNotBlank();
            assertThat(source.path("citation").asText()).isNotBlank();
            assertThat(source.path("url").asText()).startsWith("https://");
            assertThat(source.path("url").asText()).contains(".gov.vn");
        }
    }

    private String migrationSql() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V60__cu_chi_vietnamese_only_pilot_citations.sql")) {
            assertThat(input).as("V60 Củ Chi Vietnamese-only migration").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
