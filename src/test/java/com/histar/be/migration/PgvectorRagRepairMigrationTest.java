package com.histar.be.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PgvectorRagRepairMigrationTest {

    @Test
    void repairMigration_isIdempotentAndSkipsVectorColumnsWhenTheHostCannotProvidePgvector() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V58__repair_pgvector_rag_schema.sql")) {
            assertThat(input).as("V58 RAG repair migration").isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql).contains("pg_available_extensions");
            assertThat(sql).contains("CREATE EXTENSION IF NOT EXISTS vector");
            assertThat(sql).contains("ADD COLUMN IF NOT EXISTS embedding vector(768)");
            assertThat(sql).contains("rag_chunks");
            assertThat(sql).contains("semantic_cache");
        }
    }

    @Test
    void phaseTwoReconciliation_restoresNarrativeAndRewardGuards() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V59__repair_phase2_schema_constraints.sql")) {
            assertThat(input).as("V59 Phase 2 schema reconciliation migration").isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql).contains("uq_narrative_progress_user_investigation");
            assertThat(sql).contains("uq_quest_reward_ledger_idempotency");
            assertThat(sql).contains("fk_assignments_campaign");
        }
    }
}
