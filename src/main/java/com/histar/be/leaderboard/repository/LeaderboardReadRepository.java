package com.histar.be.leaderboard.repository;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** One bounded ranked snapshot; never writes progress or rewards. */
@Repository @RequiredArgsConstructor
public class LeaderboardReadRepository {
    // Published bootstrap fixtures: match BOTH reserved sample emails and the original sample credential.
    static final String LEGACY_SEED_HASH = "$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe";
    static final List<String> LEGACY_SEED_EMAILS = java.util.stream.IntStream.rangeClosed(1, 8)
            .boxed().flatMap(i -> java.util.stream.Stream.of("user" + i + "@histar.vn", "demo_user_" + i + "@histar.vn")).toList();
    private final NamedParameterJdbcTemplate jdbc;
    public record RankedRow(UUID userId, String displayName, String avatarUrl, int points,
            int rank, long ordinal, long participants, int badgeCount, long xpPeers) {
        public RankedRow(UUID userId, String displayName, String avatarUrl, int points,
                int rank, long ordinal, long participants, int badgeCount) {
            this(userId, displayName, avatarUrl, points, rank, ordinal, participants, badgeCount, 1);
        }
    }

    public List<RankedRow> read(UUID viewerId, String city, UUID orgId, UUID groupId, int limit) {
        var params = new MapSqlParameterSource().addValue("viewer", viewerId).addValue("limit", limit)
                .addValue("legacySeedHash", LEGACY_SEED_HASH).addValue("legacySeedEmails", LEGACY_SEED_EMAILS);
        String filter = "";
        if (city != null) { filter += " AND p.city = :city"; params.addValue("city", city); }
        if (orgId != null) { filter += " AND p.org_id = :org"; params.addValue("org", orgId); }
        if (groupId != null) {
            filter += " AND EXISTS (SELECT 1 FROM study_group_members m WHERE m.user_id = p.id AND m.group_id = :groupId)";
            params.addValue("groupId", groupId);
        }
        String sql = """
                WITH ranked AS (
                  SELECT p.id, COALESCE(NULLIF(TRIM(p.display_name), ''), 'Nhà thám hiểm') display_name,
                    p.avatar_url, p.total_points,
                    RANK() OVER (ORDER BY p.total_points DESC) honor_rank,
                    ROW_NUMBER() OVER (ORDER BY p.total_points DESC, COALESCE(b.badge_count, 0) DESC, p.id) ordinal,
                    COUNT(*) OVER () participants,
                    COUNT(*) OVER (PARTITION BY p.total_points) xp_peers, COALESCE(b.badge_count, 0) badge_count
                  FROM profiles p
                  LEFT JOIN (SELECT user_id, COUNT(DISTINCT badge_id) badge_count FROM user_badges GROUP BY user_id) b ON b.user_id=p.id
                  WHERE p.total_points > 0 AND UPPER(TRIM(COALESCE(p.role, 'USER'))) <> 'ADMIN'
                -- AccountStatus.fromStored: trim ASCII controls/spaces, ignore case; unknown values remain active.
                AND UPPER(REGEXP_REPLACE(REGEXP_REPLACE(COALESCE(p.account_status,''), '^[\\x01-\\x20]+', ''), '[\\x01-\\x20]+$', '')) <> 'SUSPENDED'
                    AND NOT (CAST(p.id AS VARCHAR) LIKE 'a1000000-0000-4000-8000-%'
                      AND p.email LIKE 'seed-analytics-%@histar.vn')
                    AND NOT (COALESCE(p.password_hash, '') = :legacySeedHash AND p.email IN (:legacySeedEmails))
                """ + filter + """
                )
                SELECT r.*
                FROM ranked r
                WHERE r.ordinal <= :limit OR r.id = :viewer
                  OR r.ordinal BETWEEN (SELECT ordinal FROM ranked WHERE id = :viewer) - 2
                                   AND (SELECT ordinal FROM ranked WHERE id = :viewer) + 2
                ORDER BY r.ordinal
                """;
        return jdbc.query(sql, params, (rs, rowNum) -> new RankedRow(rs.getObject("id", UUID.class),
                rs.getString("display_name"), rs.getString("avatar_url"), rs.getInt("total_points"),
                Math.toIntExact(rs.getLong("honor_rank")), rs.getLong("ordinal"), rs.getLong("participants"),
                Math.toIntExact(rs.getLong("badge_count")), rs.getLong("xp_peers")));
    }

    public int badgeCount(UUID userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(DISTINCT badge_id) FROM user_badges WHERE user_id = :viewer",
                new MapSqlParameterSource("viewer", userId), Integer.class);
        return count == null ? 0 : count;
    }
}

