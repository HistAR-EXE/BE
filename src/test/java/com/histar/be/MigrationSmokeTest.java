package com.histar.be;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MigrationSmokeTest {

    private static final List<String> CRITICAL_MIGRATIONS = List.of(
            "2026-06-12_value_layer_upgrade.sql",
            "2026-06-13_phase_b_upgrade.sql",
            "2026-06-14_phase1_hardening.sql",
            "2026-06-15_visit_session_event_snapshot.sql",
            "2026-06-16_analytics_event_metadata.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schema_hasCriticalMigrationColumns() {
        assertColumnExists("VISIT_SESSIONS", "LAST_ACTIVITY_AT");
        assertColumnExists("VISIT_SESSIONS", "ENDED_REASON");
        assertColumnExists("VISIT_SESSION_EVENTS", "POI_NAME_SNAPSHOT");
        assertColumnExists("ANALYTICS_EVENTS", "METADATA");
    }

    @Test
    void runAllSeedScript_listsAllCriticalMigrationsInOrder() throws Exception {
        Path runAllSeed = Path.of("docs/scripts/run-all-seed.ps1");
        assertThat(Files.exists(runAllSeed))
                .as("run-all-seed.ps1 must exist at BE/docs/scripts/")
                .isTrue();

        String script = Files.readString(runAllSeed);
        for (String migration : CRITICAL_MIGRATIONS) {
            assertThat(script).as("script must reference " + migration).contains(migration);
            Path sqlFile = Path.of("docs/database", migration);
            assertThat(Files.exists(sqlFile))
                    .as("migration file must exist: " + sqlFile)
                    .isTrue();
        }

        for (int i = 0; i < CRITICAL_MIGRATIONS.size() - 1; i++) {
            String current = CRITICAL_MIGRATIONS.get(i);
            String next = CRITICAL_MIGRATIONS.get(i + 1);
            int idxA = script.indexOf(current);
            int idxB = script.indexOf(next);
            assertThat(idxA).as("missing " + current).isGreaterThanOrEqualTo(0);
            assertThat(idxB).as("missing " + next).isGreaterThanOrEqualTo(0);
            assertThat(idxA).as("order: " + current + " before " + next).isLessThan(idxB);
        }
    }

    private void assertColumnExists(String tableName, String columnName) {
        List<String> columns = jdbcTemplate.queryForList(
                """
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?
                """,
                String.class,
                tableName,
                columnName);
        assertTrue(
                columns.stream().anyMatch(c -> c.equalsIgnoreCase(columnName)),
                () -> "Missing column " + tableName + "." + columnName);
    }
}
