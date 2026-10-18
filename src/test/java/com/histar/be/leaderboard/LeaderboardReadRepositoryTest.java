package com.histar.be.leaderboard;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.histar.be.leaderboard.repository.LeaderboardReadRepository;

class LeaderboardReadRepositoryTest {
    JdbcTemplate jdbc;
    LeaderboardReadRepository repository;
    UUID viewer;
    List<UUID> users;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE profiles(id UUID PRIMARY KEY, display_name VARCHAR, avatar_url VARCHAR, email VARCHAR, total_points INT, role VARCHAR, city VARCHAR, org_id UUID, password_hash VARCHAR)");
        jdbc.execute("CREATE TABLE user_badges(user_id UUID, badge_id UUID)");
        jdbc.execute("CREATE TABLE study_group_members(user_id UUID, group_id UUID)");
        repository = new LeaderboardReadRepository(new NamedParameterJdbcTemplate(source));
        users = new ArrayList<>();
        for (int i = 0; i < 15; i++) { UUID id = UUID.randomUUID(); users.add(id); insert(id, i < 2 ? 2000 : 1500 - i * 50, "USER", id + "@example.test"); }
        viewer = users.get(12);
        insert(UUID.randomUUID(), 50000, "ADMIN", "admin@example.test");
        insert(UUID.fromString("a1000000-0000-4000-8000-000000000001"), 40000, "USER", "seed-analytics-001@histar.vn");
        for (String email : List.of("user1@histar.vn", "demo_user_1@histar.vn")) {
            UUID demo = UUID.randomUUID(); insert(demo, 60000, "USER", email);
            jdbc.update("UPDATE profiles SET password_hash = ? WHERE id = ?",
                    "$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe", demo);
        }
    }
    void insert(UUID id, int xp, String role, String email) {
        jdbc.update("INSERT INTO profiles(id, display_name, email, total_points, role, city) VALUES(?, ?, ?, ?, ?, 'TP.HCM')", id, "Explorer", email, xp, role);
    }
    @Test void tiesShareRankAndStableOrderAndViewerOutsideTopIsIncluded() {
        var rows = repository.read(viewer, null, null, null, 3);
        assertThat(rows.stream().filter(r -> r.ordinal() <= 3).map(r -> r.rank()).toList()).containsExactly(1, 1, 3);
        assertThat(rows).anySatisfy(r -> { assertThat(r.userId()).isEqualTo(viewer); assertThat(r.rank()).isEqualTo(13); });
        assertThat(rows.get(0).participants()).isEqualTo(15);
        assertThat(rows).hasSize(8); // top three + five neighbors
        assertThat(repository.read(viewer, null, null, null, 3)).isEqualTo(rows);
    }
    @Test void readsPreserveProfileTotalsAndBadges() {
        Integer sum = jdbc.queryForObject("SELECT SUM(total_points) FROM profiles", Integer.class);
        jdbc.update("INSERT INTO user_badges(user_id, badge_id) VALUES(?, ?)", viewer, UUID.randomUUID());
        assertThat(repository.read(viewer, null, null, null, 3)).anySatisfy(r -> {
            assertThat(r.userId()).isEqualTo(viewer); assertThat(r.badgeCount()).isEqualTo(1);
        });
        assertThat(jdbc.queryForObject("SELECT SUM(total_points) FROM profiles", Integer.class)).isEqualTo(sum);
        assertThat(repository.badgeCount(viewer)).isEqualTo(1);
    }
    @Test void cityFiltersAreBoundNotInterpolated() {
        assertThat(repository.read(viewer, "x' OR 1=1 --", null, null, 3)).isEmpty();
    }
    @Test void groupScopeContainsOnlyMembersAndDoesNotExposeOtherParticipants() {
        UUID group = UUID.randomUUID();
        jdbc.update("INSERT INTO study_group_members VALUES(?, ?)", viewer, group);
        var rows = repository.read(viewer, null, null, group, 3);
        assertThat(rows).hasSize(1); assertThat(rows.get(0).participants()).isEqualTo(1);
        assertThat(rows.get(0).rank()).isEqualTo(1);
    }
    @Test void zeroAndNegativeBalancesAreNotCompetitiveRanks() {
        jdbc.update("UPDATE profiles SET total_points = 0 WHERE id = ?", viewer);
        insert(UUID.randomUUID(), -1, "USER", "negative@example.test");
        var rows = repository.read(viewer, null, null, null, 3);
        assertThat(rows).noneMatch(r -> r.userId().equals(viewer));
        assertThat(rows.get(0).participants()).isEqualTo(14);
    }
    @Test void similarlyNamedRealAccountIsNotRemovedAsSynthetic() {
        UUID id = UUID.randomUUID(); insert(id, 3000, "USER", "seed-analytics-real@histar.vn");
        assertThat(repository.read(id, null, null, null, 3).get(0).userId()).isEqualTo(id);
        UUID real = UUID.randomUUID(); insert(real, 3100, "USER", "user2@histar.vn");
        assertThat(repository.read(real, null, null, null, 3).get(0).userId()).isEqualTo(real);
    }
    @Test void displayTiesPreferDistinctBadgeCountThenUuidWithoutChangingRanks() {
        UUID a=UUID.fromString("00000000-0000-4000-8000-000000000001");
        UUID b=UUID.fromString("00000000-0000-4000-8000-000000000002");
        for(UUID id:List.of(a,b)) insert(id,9999,"USER",id+"@test.invalid");
        UUID badge=UUID.randomUUID();
        for(int i=0;i<2;i++) jdbc.update("INSERT INTO user_badges VALUES(?,?)",b,badge);
        var rows=repository.read(viewer,null,null,null,10);
        assertThat(rows.get(0).userId()).isEqualTo(b);
        assertThat(rows.get(0).badgeCount()).isEqualTo(1);
        assertThat(rows.get(1).userId()).isEqualTo(a);
        assertThat(rows.get(0).rank()).isEqualTo(rows.get(1).rank());
        assertThat(rows.get(0).xpPeers()).isEqualTo(2);
    }
}
