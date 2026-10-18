package com.histar.be.leaderboard.repository;
import com.histar.be.discovery.explore.ExploreBadgeRules;
import com.histar.be.discovery.explore.ExploreChallengeCatalog;
import com.histar.be.leaderboard.dto.HonorsPassportResponse.HeritageBadge;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

/** Confirmed owner and scoped community evidence. All operations are reads. */
@Repository @RequiredArgsConstructor
public class HonorsFactsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public record Counts(long explorers,long completedChallenges,long discoveries,long badges) {}
    public int badgeCount(UUID user) {
        return jdbc.queryForObject("SELECT COUNT(DISTINCT badge_id) FROM user_badges WHERE user_id=:viewer",
                new MapSqlParameterSource("viewer",user),Integer.class);
    }
    public List<HeritageBadge> heritageBadges(UUID user) {
        Map<UUID,String> slugs=new HashMap<>();
        for(int i=0;i<ExploreBadgeRules.IDS.size();i++) slugs.put(ExploreBadgeRules.databaseId(i),ExploreBadgeRules.IDS.get(i));
        return jdbc.query("SELECT badge_id, MIN(earned_at) earned_at FROM user_badges WHERE user_id=:viewer AND badge_id IN (:ids) GROUP BY badge_id ORDER BY badge_id",
            new MapSqlParameterSource("viewer",user).addValue("ids",slugs.keySet()),
            (rs,n)->new HeritageBadge(slugs.get(rs.getObject("badge_id",UUID.class)),
                rs.getTimestamp("earned_at")==null?null:rs.getTimestamp("earned_at").toInstant()));
    }
    public Counts community(String city, UUID org, UUID group) {
        var p=new MapSqlParameterSource("location",ExploreChallengeCatalog.LOCATION)
            .addValue("keys",HonorsStoriesRepository.catalogKeys())
            .addValue("legacySeedHash",LeaderboardReadRepository.LEGACY_SEED_HASH)
            .addValue("legacySeedEmails",LeaderboardReadRepository.LEGACY_SEED_EMAILS);
        String filter="";
        if(city!=null){filter+=" AND p.city=:city";p.addValue("city",city);}
        if(org!=null){filter+=" AND p.org_id=:org";p.addValue("org",org);}
        if(group!=null){filter+=" AND EXISTS (SELECT 1 FROM study_group_members m WHERE m.user_id=p.id AND m.group_id=:groupId)";p.addValue("groupId",group);}
        String sql="""
            WITH eligible AS (
                SELECT p.id FROM profiles p WHERE COALESCE(p.role,'USER')<>'ADMIN'
                -- AccountStatus.fromStored: trim ASCII controls/spaces, ignore case; unknown values remain active.
                AND UPPER(REGEXP_REPLACE(REGEXP_REPLACE(COALESCE(p.account_status,''), '^[\\x01-\\x20]+', ''), '[\\x01-\\x20]+$', '')) <> 'SUSPENDED'
                AND COALESCE(p.provider,'')<>'ui-preview-honors'
                AND NOT (CAST(p.id AS VARCHAR) LIKE 'a1000000-0000-4000-8000-%' AND COALESCE(p.email,'') LIKE 'seed-analytics-%@histar.vn')
                AND NOT (COALESCE(p.password_hash,'')=:legacySeedHash AND COALESCE(p.email,'') IN (:legacySeedEmails))
            """+filter+"""
            ), challenges AS (
                SELECT DISTINCT d.user_id,d.location_id,d.discovery_key FROM user_discoveries d JOIN eligible e ON e.id=d.user_id
                WHERE d.location_id=:location AND d.discovery_key IN (:keys)
            ), discoveries AS (
                SELECT DISTINCT d.user_id,d.location_id FROM user_discoveries d JOIN eligible e ON e.id=d.user_id JOIN locations l ON l.id=d.location_id
            ), earned AS (
                SELECT DISTINCT b.user_id,b.badge_id FROM user_badges b JOIN eligible e ON e.id=b.user_id
            )
            SELECT (SELECT COUNT(*) FROM eligible e JOIN profiles p ON p.id=e.id WHERE p.total_points>0
                OR EXISTS(SELECT 1 FROM user_discoveries d WHERE d.user_id=e.id)
                OR EXISTS(SELECT 1 FROM user_badges b WHERE b.user_id=e.id)) explorers,
                (SELECT COUNT(*) FROM challenges) completed_challenges,
                (SELECT COUNT(*) FROM discoveries) discoveries,
                (SELECT COUNT(*) FROM earned) badges
            """;
        return jdbc.queryForObject(sql,p,(rs,n)->new Counts(rs.getLong("explorers"),rs.getLong("completed_challenges"),rs.getLong("discoveries"),rs.getLong("badges")));
    }
}
