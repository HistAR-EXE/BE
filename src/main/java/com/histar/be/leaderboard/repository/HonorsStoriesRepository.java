package com.histar.be.leaderboard.repository;

import com.histar.be.discovery.explore.ExploreChallengeCatalog;
import com.histar.be.leaderboard.dto.HonorsStoriesResponse.Timeline;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

/** Bounded evidence reads; never awards, updates, or locks progress. */
@Repository @RequiredArgsConstructor
public class HonorsStoriesRepository {
 private final NamedParameterJdbcTemplate jdbc;
 public record Metric(String category,UUID userId,String displayName,String avatarUrl,int count,int rank,long ordinal,long achievers) {}
 private MapSqlParameterSource params(UUID viewer) {
  return new MapSqlParameterSource("viewer",viewer).addValue("location",ExploreChallengeCatalog.LOCATION)
    .addValue("keys",catalogKeys()).addValue("legacySeedHash",LeaderboardReadRepository.LEGACY_SEED_HASH)
    .addValue("legacySeedEmails",LeaderboardReadRepository.LEGACY_SEED_EMAILS);
 }
 public static List<String> catalogKeys() {
  var keys=new ArrayList<>(ExploreChallengeCatalog.QUIZZES.keySet());keys.addAll(ExploreChallengeCatalog.CLUES);keys.add(ExploreChallengeCatalog.CONCLUSION);return keys;
 }
 public Map<String,java.time.Instant> completionDates(UUID viewer) {
  Map<String,java.time.Instant> dates = new HashMap<>();
  jdbc.query("SELECT discovery_key, discovered_at FROM user_discoveries WHERE user_id=:viewer AND location_id=:location AND discovery_key IN (:keys)",params(viewer),
   (org.springframework.jdbc.core.RowCallbackHandler) rs -> dates.put(rs.getString("discovery_key"),
    rs.getTimestamp("discovered_at") == null ? null : rs.getTimestamp("discovered_at").toInstant()));
  return dates;
 }
 public List<Timeline> timeline(UUID viewer) {
  String sql="""
   SELECT * FROM (
     SELECT CONCAT('discovery:', d.discovery_key) event_id, d.discovery_key title,
       'Thử thách đã được ghi nhận' description, d.discovered_at occurred_at, 'discovery' icon
     FROM user_discoveries d WHERE d.user_id=:viewer AND d.location_id=:location AND d.discovery_key IN (:keys)
     UNION ALL
     SELECT CONCAT('badge:', CAST(b.badge_id AS VARCHAR)), COALESCE(NULLIF(TRIM(c.name),''),'Huy hiệu hành trình'),
       COALESCE(NULLIF(TRIM(c.description),''),'Huy hiệu đã được ghi nhận'), b.earned_at, 'badge'
     FROM user_badges b LEFT JOIN badges c ON c.id=b.badge_id WHERE b.user_id=:viewer
   ) events ORDER BY CASE WHEN occurred_at IS NULL THEN 1 ELSE 0 END, occurred_at DESC, event_id LIMIT 60
   """;
  return jdbc.query(sql,params(viewer),(rs,n)->new Timeline(rs.getString("event_id"),rs.getString("title"),rs.getString("description"),
    rs.getTimestamp("occurred_at")==null?null:rs.getTimestamp("occurred_at").toInstant(),"/explore",rs.getString("icon")));
 }
 public List<Metric> metrics(UUID viewer,String city,UUID org,UUID group) {
  var p=params(viewer);String filter="";
  if(city!=null){filter+=" AND p.city=:city";p.addValue("city",city);}
  if(org!=null){filter+=" AND p.org_id=:org";p.addValue("org",org);}
  if(group!=null){filter+=" AND EXISTS (SELECT 1 FROM study_group_members m WHERE m.user_id=p.id AND m.group_id=:groupId)";p.addValue("groupId",group);}
  String sql="""
   WITH eligible AS (
    SELECT p.id, COALESCE(NULLIF(TRIM(p.display_name),''),'Nhà thám hiểm') display_name,p.avatar_url
    FROM profiles p WHERE p.total_points>0 AND COALESCE(p.role,'USER')<>'ADMIN'
     AND NOT (CAST(p.id AS VARCHAR) LIKE 'a1000000-0000-4000-8000-%' AND p.email LIKE 'seed-analytics-%@histar.vn')
     AND NOT (COALESCE(p.password_hash,'')=:legacySeedHash AND p.email IN (:legacySeedEmails))
   """+filter+"""
   ), discovery_counts AS (
    SELECT d.user_id,
     COUNT(DISTINCT CASE WHEN d.location_id=:location AND d.discovery_key IN ('cipher:hoang-cam','cipher:two-wells','cipher:sandals') THEN d.discovery_key END) ciphers,
     COUNT(DISTINCT CASE WHEN l.id IS NOT NULL THEN d.location_id END) locations,
     COUNT(DISTINCT CASE WHEN d.location_id=:location AND d.discovery_key IN ('depth_quiz:surface','depth_quiz:layer1','depth_quiz:layer2','depth_quiz:layer3') THEN d.discovery_key END) depths,
     COUNT(DISTINCT CASE WHEN d.location_id=:location AND d.discovery_key IN ('clue:ev-01','clue:ev-02','clue:ev-03','clue:ev-04','investigation:cu-chi-conclusion') THEN d.discovery_key END) evidence,
     COUNT(DISTINCT CASE WHEN d.location_id=:location AND d.discovery_key IN ('dossier:architecture','dossier:trap-warfare','dossier:underground-life','dossier:command-center','dossier:tunnel-rats','dossier:ben-duoc') THEN d.discovery_key END) dossiers
    FROM user_discoveries d JOIN eligible e ON e.id=d.user_id LEFT JOIN locations l ON l.id=d.location_id GROUP BY d.user_id
   ), badge_counts AS (SELECT b.user_id,COUNT(DISTINCT b.badge_id) badges FROM user_badges b JOIN eligible e ON e.id=b.user_id GROUP BY b.user_id),
   counts AS (
    SELECT e.*,COALESCE(d.ciphers,0) ciphers,COALESCE(d.locations,0) locations,COALESCE(b.badges,0) badges,
     (CASE WHEN d.depths=4 THEN 1 ELSE 0 END + CASE WHEN d.ciphers=3 THEN 1 ELSE 0 END + CASE WHEN d.evidence=5 THEN 1 ELSE 0 END + CASE WHEN d.dossiers=6 THEN 1 ELSE 0 END) sets
    FROM eligible e LEFT JOIN discovery_counts d ON d.user_id=e.id LEFT JOIN badge_counts b ON b.user_id=e.id
   ), metrics AS (
    SELECT id,display_name,avatar_url,'ciphers' category,ciphers metric_count FROM counts UNION ALL
    SELECT id,display_name,avatar_url,'locations',locations FROM counts UNION ALL
    SELECT id,display_name,avatar_url,'challenge-sets',sets FROM counts UNION ALL
    SELECT id,display_name,avatar_url,'badges',badges FROM counts
   ), ranked AS (
    SELECT *,RANK() OVER (PARTITION BY category ORDER BY metric_count DESC) honor_rank,
     ROW_NUMBER() OVER (PARTITION BY category ORDER BY metric_count DESC,id) ordinal,
     COUNT(*) OVER (PARTITION BY category) achievers FROM metrics WHERE metric_count>0
   ) SELECT * FROM ranked WHERE ordinal<=3 OR id=:viewer ORDER BY category,ordinal
   """;
  return jdbc.query(sql,p,(rs,n)->new Metric(rs.getString("category"),rs.getObject("id",UUID.class),rs.getString("display_name"),rs.getString("avatar_url"),rs.getInt("metric_count"),rs.getInt("honor_rank"),rs.getLong("ordinal"),rs.getLong("achievers")));
 }
}

