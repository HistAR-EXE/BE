package com.histar.be.siteworkspace;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class SiteWorkspaceMigrationContractTest {

    private static final String MIGRATION =
            "/db/migration/V91__site_workspaces_and_content_versions.sql";

    @Test
    void v91CreatesWorkspaceTablesAndColumns() throws Exception {
        String sql = migrationSql();

        assertThat(sql)
                .contains("create table site_memberships")
                .contains("create table site_onboarding_requests")
                .contains("create table site_workspace_settings")
                .contains("create table site_content_versions")
                .contains("site_code varchar(64) not null")
                .contains("user_id uuid not null")
                .contains("role varchar(32) not null")
                .contains("active boolean not null default true")
                .contains("requires_trip_confirmation boolean not null default false")
                .contains("content_kind varchar(32) not null")
                .contains("resource_key varchar(128) not null")
                .contains("location_id uuid")
                .contains("station_id uuid")
                .contains("snapshot_json jsonb not null")
                .contains("version integer not null")
                .contains("status varchar(32) not null default 'draft'")
                .contains("revision bigint not null default 0")
                .doesNotContain("bytea", " blob ");
    }

    @Test
    void v91EnforcesWorkspaceRelationshipsLifecyclesAndLookupIndexes() throws Exception {
        String sql = migrationSql();

        assertThat(sql)
                .contains("update locations set site_code = null where site_code = ''")
                .contains("unique (site_code, user_id)")
                .contains("unique (site_code)")
                .contains("unique (site_code, content_kind, resource_key, version)")
                .contains("references locations(site_code)")
                .contains("references locations(id)")
                .contains("references stations(id)")
                .contains("references profiles(id)")
                .contains("check (role in ('site_manager', 'content_editor', 'content_reviewer'))")
                .contains("check (status in ('pending', 'approved', 'rejected'))")
                .contains("check (content_kind in ('site_profile', 'station'))")
                .contains("check (status in ('draft', 'in_review', 'approved', 'published', 'archived'))")
                .contains("check ((content_kind = 'site_profile' and location_id is not null and station_id is null) or (content_kind = 'station' and location_id is null and station_id is not null))")
                .contains("snapshot_json ?& array['stationcode', 'name', 'sortorder', 'lat', 'lng', 'queststepkey', 'active', 'blocks']")
                .contains("jsonb_typeof(snapshot_json -> 'blocks') = 'array'")
                .contains("create index idx_site_onboarding_requests_site_status on site_onboarding_requests (site_code, status)")
                .contains("create index idx_site_content_versions_lookup on site_content_versions (site_code, content_kind, resource_key, status)");
    }

    @Test
    void domainTypesAndRepositoriesMatchTheMigrationContract() throws Exception {
        assertEnumValues(
                "com.histar.be.siteworkspace.entity.SiteRole",
                "SITE_MANAGER", "CONTENT_EDITOR", "CONTENT_REVIEWER");
        assertEnumValues(
                "com.histar.be.siteworkspace.entity.SiteOnboardingStatus",
                "PENDING", "APPROVED", "REJECTED");
        assertEnumValues(
                "com.histar.be.siteworkspace.entity.SiteContentKind",
                "SITE_PROFILE", "STATION");
        assertEnumValues(
                "com.histar.be.siteworkspace.entity.ContentLifecycleStatus",
                "DRAFT", "IN_REVIEW", "APPROVED", "PUBLISHED", "ARCHIVED");

        assertEntityTable("SiteMembership", "site_memberships");
        assertEntityTable("SiteOnboardingRequest", "site_onboarding_requests");
        assertEntityTable("SiteWorkspaceSettings", "site_workspace_settings");
        assertEntityTable("SiteContentVersion", "site_content_versions");
        assertUuidId("SiteMembership");
        assertUuidId("SiteOnboardingRequest");
        assertUuidId("SiteWorkspaceSettings");
        assertUuidId("SiteContentVersion");

        assertJpaRepository("SiteMembershipRepository");
        assertJpaRepository("SiteOnboardingRequestRepository");
        assertJpaRepository("SiteWorkspaceSettingsRepository");
        assertJpaRepository("SiteContentVersionRepository");
    }

    private String migrationSql() throws Exception {
        try (var input = getClass().getResourceAsStream(MIGRATION)) {
            assertThat(input).as("V91 site workspace migration").isNotNull();
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

    private void assertEntityTable(String simpleName, String tableName) throws Exception {
        Class<?> entityType = Class.forName("com.histar.be.siteworkspace.entity." + simpleName);
        assertThat(entityType.getAnnotation(Table.class))
                .extracting(Table::name)
                .isEqualTo(tableName);
    }

    private void assertJpaRepository(String simpleName) throws Exception {
        Class<?> repositoryType = Class.forName("com.histar.be.siteworkspace.repository." + simpleName);
        assertThat(JpaRepository.class.isAssignableFrom(repositoryType)).isTrue();
    }

    private void assertUuidId(String simpleName) throws Exception {
        Class<?> entityType = Class.forName("com.histar.be.siteworkspace.entity." + simpleName);
        assertThat(entityType.getDeclaredField("id").getType()).isEqualTo(java.util.UUID.class);
    }
}
