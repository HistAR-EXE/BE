package com.histar.be.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AdminPhase12MigrationTest {

    @Test
    void v69CreatesDurableContentReportCasesAndBillingSnapshotColumns() throws IOException {
        String sql = migrationSql().toLowerCase();

        assertThat(sql)
                .contains("create table content_report_cases")
                .contains("event_id uuid")
                .contains("references pilot_events(id)")
                .contains("status in ('new', 'in_review', 'resolved', 'rejected')")
                .contains("insert into content_report_cases")
                .contains("where event_type = 'content_report'")
                .contains("license_count")
                .contains("gross_amount_vnd")
                .contains("discount_amount_vnd")
                .contains("subscription_id")
                .contains("source_payment_transaction_id")
                .contains("billing_cadence")
                .contains("contract_value_vnd")
                .contains("currency")
                .contains("quantity");
        assertThat(sql).doesNotContain("payload_json like").doesNotContain("payload_json ilike");
    }

    private String migrationSql() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V69__admin_console_phase_1_2.sql")) {
            assertThat(input).as("V69 admin console phase 1-2 migration").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
