package com.histar.be.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Opt-in PostgreSQL proof that V88 repairs already-persisted question marks and is idempotent. */
@EnabledIfEnvironmentVariable(named = "RUN_VIETNAMESE_REPAIR_POSTGRES_IT", matches = "true")
class VietnameseDataRepairPostgresTest {

    @Test
    void repairsCorruptedCuChiLocationAndQuestText() throws Exception {
        String schema = "vietnamese_repair_test_" + UUID.randomUUID().toString().replace("-", "");
        var dataSource = new DriverManagerDataSource(
                System.getenv("VIETNAMESE_REPAIR_TEST_DB_URL"),
                System.getenv("VIETNAMESE_REPAIR_TEST_DB_USER"),
                System.getenv("VIETNAMESE_REPAIR_TEST_DB_PASSWORD"));
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA " + schema);
        var connectionProperties = new Properties();
        connectionProperties.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(connectionProperties);
        try {
            jdbc.execute("CREATE TABLE " + schema
                    + ".locations (id uuid PRIMARY KEY, name text, description text, latitude double precision,"
                    + " longitude double precision, city text, cover_image text)");
            jdbc.execute("CREATE TABLE " + schema
                    + ".artifacts (id uuid PRIMARY KEY, location_id uuid REFERENCES " + schema
                    + ".locations(id), name text, image_url text, description text, story text,"
                    + " unlock_key text, reliability text, sort_order integer,"
                    + " UNIQUE(location_id, unlock_key))");
            jdbc.execute("CREATE TABLE " + schema
                    + ".quests (id uuid PRIMARY KEY, title text, description text, story text)");
            jdbc.execute("CREATE TABLE " + schema
                    + ".discovery_points (id uuid PRIMARY KEY, location_id uuid REFERENCES " + schema
                    + ".locations(id), name text, map_x_pct numeric, map_y_pct numeric, unlock_key text,"
                    + " sort_order integer, UNIQUE(location_id, unlock_key))");
            jdbc.update("INSERT INTO " + schema + ".locations(id, name, description) VALUES (?::uuid, ?, ?)",
                    "11111111-1111-1111-1111-111111111111", "??a ??o C? Chi", "T?ng quan b? l?i");
            jdbc.update("INSERT INTO " + schema + ".quests VALUES (?::uuid, ?, ?, ?)",
                    "33333333-3333-3333-3333-333333333333",
                    "H?nh tr?nh d??i l?ng ??t", "Mô tả mới đã đúng dấu", "N?m 1968, gi?a l?ng ??t");
            jdbc.update("INSERT INTO " + schema + ".quests VALUES (?::uuid, ?, ?, ?)",
                    "33333333-3333-3333-3333-333333333334",
                    "M?t m? Sa b?n T?c chi?n", "T?m hi?u c?u tr?c", "T?i C? Chi");
            jdbc.update("INSERT INTO " + schema + ".quests VALUES (?::uuid, ?, ?, ?)",
                    "33333333-3333-3333-3333-333333333335",
                    "?nh s?ng ??n M? U", "Gi?i m? cu?c s?ng", "D??i nh?ng l?p ??t");

            jdbc.update("INSERT INTO " + schema
                            + ".discovery_points(id, location_id, name, unlock_key) VALUES (?::uuid, ?::uuid, ?, ?)",
                    "d0000001-0000-4000-8000-000000000002",
                    "11111111-1111-1111-1111-111111111111", "B?p Ho?ng C?m",
                    "scene:22222222-2222-2222-2222-222222222221");

            String migration = Files.readString(
                    Path.of("src/main/resources/db/migration/V88__repair_vietnamese_content.sql"),
                    StandardCharsets.UTF_8);
            jdbc.execute(migration);
            jdbc.execute(migration);

            assertThat(jdbc.queryForObject(
                    "SELECT name FROM " + schema + ".locations WHERE id = ?::uuid",
                    String.class, "11111111-1111-1111-1111-111111111111"))
                    .isEqualTo("Địa đạo Củ Chi");
            assertThat(jdbc.queryForObject(
                    "SELECT description FROM " + schema + ".locations WHERE id = ?::uuid",
                    String.class, "11111111-1111-1111-1111-111111111111"))
                    .contains("Tổng quan & Vị trí")
                    .doesNotContain("?");
            assertThat(jdbc.queryForList(
                    "SELECT title || description || story FROM " + schema
                            + ".quests WHERE id::text LIKE '33333333-3333-3333-3333-33333333333%'",
                    String.class))
                    .allMatch(value -> !value.contains("?"));
            assertThat(jdbc.queryForObject(
                    "SELECT description FROM " + schema + ".quests WHERE id = ?::uuid",
                    String.class, "33333333-3333-3333-3333-333333333333"))
                    .isEqualTo("Mô tả mới đã đúng dấu");
            assertThat(jdbc.queryForObject(
                    "SELECT name FROM " + schema + ".discovery_points WHERE unlock_key = ?",
                    String.class, "scene:22222222-2222-2222-2222-222222222221"))
                    .isEqualTo("Bếp Hoàng Cầm");
        } finally {
            if (!schema.matches("vietnamese_repair_test_[0-9a-f]{32}")) {
                throw new IllegalStateException("Unexpected test schema");
            }
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
}
