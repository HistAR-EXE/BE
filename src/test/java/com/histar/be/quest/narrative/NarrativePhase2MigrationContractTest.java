package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NarrativePhase2MigrationContractTest {

    @Test
    void v68CreatesVersionedStorageRolloutPointerAndSafeBackfill() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V68__narrative_investigation_versioning.sql"));

        assertThat(sql)
                .contains("CREATE TABLE narrative_investigation_versions")
                .contains("published_version_id")
                .contains("rollout_state")
                .contains("NOT_RELEASED", "ACTIVE", "PAUSED")
                .contains("CREATE UNIQUE INDEX uq_narrative_open_draft")
                .contains("CREATE TABLE narrative_rollout_idempotency")
                .contains("audit_event_id")
                .contains("INSERT INTO narrative_investigation_versions")
                .contains("WHEN ni.enabled AND ni.review_status = 'APPROVED' THEN 'ACTIVE'")
                .contains("WHEN NOT ni.enabled AND ni.review_status = 'APPROVED' THEN 'PAUSED'")
                .contains("ELSE 'NOT_RELEASED'");
    }
}
