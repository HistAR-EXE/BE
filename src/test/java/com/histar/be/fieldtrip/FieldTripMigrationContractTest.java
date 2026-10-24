package com.histar.be.fieldtrip;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class FieldTripMigrationContractTest {

    private static final String MIGRATION = "/db/migration/V92__field_trip_workflow.sql";

    @Test
    void v92CreatesFieldTripTablesAndColumns() throws Exception {
        String sql = migrationSql();

        assertThat(sql)
                .contains("create table field_trips")
                .contains("id uuid primary key")
                .contains("org_id uuid not null")
                .contains("site_code varchar(64) not null")
                .contains("title varchar(255) not null")
                .contains("visit_start timestamptz not null")
                .contains("visit_end timestamptz not null")
                .contains("quest_id uuid not null")
                .contains("campaign_id uuid")
                .contains("join_code varchar(32) not null")
                .contains("status varchar(32) not null default 'draft'")
                .contains("site_confirmation_status varchar(32) not null default 'not_required'")
                .contains("site_note text")
                .contains("created_by uuid not null")
                .contains("created_at timestamptz not null")
                .contains("updated_at timestamptz not null")
                .contains("row_version bigint not null default 0")
                .contains("create table field_trip_report_snapshots")
                .contains("generated_at timestamptz not null")
                .contains("report_json jsonb not null")
                .doesNotContain("student_email", "student_name", "bytea", " blob ");
    }

    @Test
    void v92EnforcesRelationshipsChecksAndIndexes() throws Exception {
        String sql = migrationSql();

        assertThat(sql)
                .contains("references organizations(id)")
                .contains("references locations(site_code)")
                .contains("references quests(id)")
                .contains("references quest_campaigns(id)")
                .contains("references profiles(id)")
                .contains("check (site_code = lower(site_code))")
                .contains("check (visit_end > visit_start)")
                .contains("check (status in ('draft', 'pending_site', 'confirmed', 'active', 'completed', 'cancelled'))")
                .contains("check (site_confirmation_status in ('not_required', 'pending', 'confirmed', 'declined'))")
                .contains("create unique index uq_field_trips_join_code_ci on field_trips (lower(join_code))")
                .contains("create index idx_field_trips_org_id on field_trips (org_id)")
                .contains("create index idx_field_trips_site_code on field_trips (site_code)")
                .contains("create index idx_field_trips_status on field_trips (status)");
    }

    @Test
    void v92LinksGroupsAndAssignmentsWithoutRewritingExistingRows() throws Exception {
        String sql = migrationSql();

        assertThat(sql)
                .contains("alter table study_groups")
                .contains("add column field_trip_id uuid")
                .contains("references field_trips(id)")
                .contains("add column participation_mode varchar(32)")
                .contains("add column declared_size integer")
                .contains("check (participation_mode in ('individual', 'shared_device'))")
                .contains("alter table assignments")
                .contains("create unique index uq_assignments_field_trip_id on assignments (field_trip_id)")
                .contains("create index idx_study_groups_field_trip_id on study_groups (field_trip_id)")
                .doesNotContain("drop column team_mode")
                .doesNotContain("update assignments")
                .doesNotContain("update study_groups set field_trip_id");
    }

    @Test
    void v92StoresOneSnapshotPerTrip() throws Exception {
        String sql = migrationSql();

        assertThat(sql)
                .contains("constraint uq_field_trip_report_snapshots_field_trip unique (field_trip_id)")
                .contains("report_json jsonb not null");
    }

    @Test
    void domainTypesAndRepositoriesMatchTheMigrationContract() throws Exception {
        assertEnumValues(
                "com.histar.be.fieldtrip.entity.FieldTripStatus",
                "DRAFT", "PENDING_SITE", "CONFIRMED", "ACTIVE", "COMPLETED", "CANCELLED");
        assertEnumValues(
                "com.histar.be.fieldtrip.entity.SiteConfirmationStatus",
                "NOT_REQUIRED", "PENDING", "CONFIRMED", "DECLINED");
        assertEnumValues(
                "com.histar.be.group.entity.ParticipationMode",
                "INDIVIDUAL", "SHARED_DEVICE");

        assertEntityTable("com.histar.be.fieldtrip.entity.FieldTrip", "field_trips");
        assertEntityTable(
                "com.histar.be.fieldtrip.entity.FieldTripReportSnapshot", "field_trip_report_snapshots");
        assertEntityTable("com.histar.be.group.entity.StudyGroup", "study_groups");
        assertEntityTable("com.histar.be.lms.entity.Assignment", "assignments");

        assertUuidId("com.histar.be.fieldtrip.entity.FieldTrip");
        assertUuidId("com.histar.be.fieldtrip.entity.FieldTripReportSnapshot");

        assertJpaRepository("com.histar.be.fieldtrip.repository.FieldTripRepository");
        assertJpaRepository("com.histar.be.fieldtrip.repository.FieldTripReportSnapshotRepository");
        assertJpaRepository("com.histar.be.group.repository.StudyGroupRepository");
        assertJpaRepository("com.histar.be.lms.repository.AssignmentRepository");

        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "orgId", "org_id");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "siteCode", "site_code");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "visitStart", "visit_start");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "visitEnd", "visit_end");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "questId", "quest_id");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "campaignId", "campaign_id");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "joinCode", "join_code");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "siteConfirmationStatus", "site_confirmation_status");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "siteNote", "site_note");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "createdBy", "created_by");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTrip", "rowVersion", "row_version");
        assertColumn("com.histar.be.group.entity.StudyGroup", "fieldTripId", "field_trip_id");
        assertColumn("com.histar.be.group.entity.StudyGroup", "participationMode", "participation_mode");
        assertColumn("com.histar.be.group.entity.StudyGroup", "declaredSize", "declared_size");
        assertColumn("com.histar.be.group.entity.StudyGroup", "teamMode", "team_mode");
        assertColumn("com.histar.be.lms.entity.Assignment", "fieldTripId", "field_trip_id");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTripReportSnapshot", "fieldTripId", "field_trip_id");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTripReportSnapshot", "generatedAt", "generated_at");
        assertColumn("com.histar.be.fieldtrip.entity.FieldTripReportSnapshot", "reportJson", "report_json");

        Class<?> fieldTrip = Class.forName("com.histar.be.fieldtrip.entity.FieldTrip");
        assertThat(fieldTrip.getDeclaredField("rowVersion").isAnnotationPresent(Version.class)).isTrue();
        assertEnumeratedString(fieldTrip, "status");
        assertEnumeratedString(fieldTrip, "siteConfirmationStatus");
        assertEnumeratedString(Class.forName("com.histar.be.group.entity.StudyGroup"), "participationMode");

        Class<?> fieldTripStatus = Class.forName("com.histar.be.fieldtrip.entity.FieldTripStatus");
        Class<?> trips = Class.forName("com.histar.be.fieldtrip.repository.FieldTripRepository");
        Method joinLookup = trips.getMethod("findByJoinCodeIgnoreCase", String.class);
        assertThat(joinLookup.getReturnType()).isEqualTo(Optional.class);

        Class<?> groups = Class.forName("com.histar.be.group.repository.StudyGroupRepository");
        Method groupsForTrip = groups.getMethod("findByFieldTripId", UUID.class);
        assertThat(List.class.isAssignableFrom(groupsForTrip.getReturnType())).isTrue();

        Class<?> assignments = Class.forName("com.histar.be.lms.repository.AssignmentRepository");
        Method assignmentForTrip = assignments.getMethod("findByFieldTripId", UUID.class);
        assertThat(assignmentForTrip.getReturnType()).isEqualTo(Optional.class);

        Class<?> snapshots = Class.forName("com.histar.be.fieldtrip.repository.FieldTripReportSnapshotRepository");
        Method snapshotForTrip = snapshots.getMethod("findByFieldTripId", UUID.class);
        assertThat(snapshotForTrip.getReturnType()).isEqualTo(Optional.class);
        assertThat(fieldTripStatus.isEnum()).isTrue();
    }

    @Test
    void fieldTripRefreshesUpdatedAtAndSnapshotStaysImmutable() throws Exception {
        assertPreUpdateCallback("com.histar.be.fieldtrip.entity.FieldTrip");

        Class<?> snapshot = Class.forName("com.histar.be.fieldtrip.entity.FieldTripReportSnapshot");
        boolean hasPreUpdate = Arrays.stream(snapshot.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(PreUpdate.class));
        assertThat(hasPreUpdate).isFalse();
        assertThat(Arrays.stream(snapshot.getDeclaredFields()).map(Field::getName))
                .doesNotContain("updatedAt");
    }

    private String migrationSql() throws Exception {
        try (var input = getClass().getResourceAsStream(MIGRATION)) {
            assertThat(input).as("V92 field trip migration").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }

    private void assertEnumValues(String className, String... expected) throws Exception {
        Class<?> enumType = Class.forName(className);
        assertThat(enumType.isEnum()).isTrue();
        List<String> actual = Arrays.stream(enumType.getEnumConstants())
                .map(value -> ((Enum<?>) value).name())
                .toList();
        assertThat(actual).containsExactly(expected);
    }

    private void assertEntityTable(String className, String tableName) throws Exception {
        Class<?> entityType = Class.forName(className);
        assertThat(entityType.getAnnotation(Table.class))
                .extracting(Table::name)
                .isEqualTo(tableName);
    }

    private void assertJpaRepository(String className) throws Exception {
        Class<?> repositoryType = Class.forName(className);
        assertThat(JpaRepository.class.isAssignableFrom(repositoryType)).isTrue();
    }

    private void assertUuidId(String className) throws Exception {
        Class<?> entityType = Class.forName(className);
        assertThat(entityType.getDeclaredField("id").getType()).isEqualTo(UUID.class);
    }

    private void assertColumn(String className, String fieldName, String columnName) throws Exception {
        Class<?> entityType = Class.forName(className);
        assertThat(entityType.getDeclaredField(fieldName).getAnnotation(Column.class))
                .extracting(Column::name)
                .isEqualTo(columnName);
    }

    private void assertEnumeratedString(Class<?> entityType, String fieldName) throws Exception {
        Enumerated enumerated = entityType.getDeclaredField(fieldName).getAnnotation(Enumerated.class);
        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    private void assertPreUpdateCallback(String className) throws Exception {
        Class<?> entityType = Class.forName(className);
        Method callback = Arrays.stream(entityType.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreUpdate.class))
                .findFirst()
                .orElseThrow();
        Object entity = entityType.getConstructor().newInstance();
        var updatedAt = entityType.getDeclaredField("updatedAt");
        updatedAt.setAccessible(true);
        updatedAt.set(entity, Instant.EPOCH);
        callback.setAccessible(true);
        callback.invoke(entity);
        assertThat((Instant) updatedAt.get(entity))
                .as(entityType.getSimpleName() + " updatedAt")
                .isAfter(Instant.EPOCH);
    }
}
