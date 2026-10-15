package com.histar.be.analytics.repository;

import com.histar.be.discovery.entity.DiscoveryPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAnalyticsRepository extends JpaRepository<DiscoveryPoint, UUID> {
    @Query(value = """
            SELECT dp.unlock_key, dp.name, COUNT(DISTINCT ud.user_id)
            FROM discovery_points dp
            LEFT JOIN user_discoveries ud ON ud.discovery_key = dp.unlock_key AND ud.location_id = dp.location_id
              AND ud.discovered_at >= :from AND ud.discovered_at < :to
            WHERE (:locationId IS NULL OR dp.location_id = :locationId) AND dp.deleted_at IS NULL
            GROUP BY dp.unlock_key, dp.name, dp.sort_order ORDER BY dp.sort_order
            """, nativeQuery = true)
    List<Object[]> poiUnlockCounts(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT dp.unlock_key, dp.name, COUNT(DISTINCT vs.user_id)
            FROM discovery_points dp
            LEFT JOIN visit_sessions vs ON vs.location_id = dp.location_id
            LEFT JOIN visit_session_events vse ON vse.visit_session_id = vs.id AND vse.event_type = 'discovery'
              AND vse.event_key = dp.unlock_key AND vse.created_at >= :from AND vse.created_at < :to
            WHERE (:locationId IS NULL OR dp.location_id = :locationId) AND dp.deleted_at IS NULL
            GROUP BY dp.unlock_key, dp.name, dp.sort_order ORDER BY dp.sort_order
            """, nativeQuery = true)
    List<Object[]> poiVisitCounts(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT q.id, q.title,
              COUNT(DISTINCT CASE WHEN upper(p.status) IN ('IN_PROGRESS','COMPLETED') THEN p.user_id END),
              COUNT(DISTINCT CASE WHEN upper(p.status) = 'COMPLETED' THEN p.user_id END)
            FROM quests q LEFT JOIN user_quest_progress p ON p.quest_id = q.id
              AND COALESCE(p.completed_at, p.started_at) >= :from AND COALESCE(p.completed_at, p.started_at) < :to
            WHERE (:locationId IS NULL OR q.location_id = :locationId) AND q.deleted_at IS NULL
            GROUP BY q.id, q.title ORDER BY q.title
            """, nativeQuery = true)
    List<Object[]> questFunnel(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT
              (SELECT COUNT(DISTINCT ud.user_id) FROM user_discoveries ud WHERE (:locationId IS NULL OR ud.location_id = :locationId) AND ud.discovered_at >= :from AND ud.discovered_at < :to),
              (SELECT COUNT(DISTINCT c.user_id) FROM checkins c WHERE (:locationId IS NULL OR c.location_id = :locationId) AND c.created_at >= :from AND c.created_at < :to),
              (SELECT COUNT(DISTINCT ud.user_id) FROM user_discoveries ud JOIN checkins c ON c.user_id = ud.user_id AND c.location_id = ud.location_id AND ud.discovered_at <= c.created_at WHERE (:locationId IS NULL OR ud.location_id = :locationId) AND ud.discovered_at >= :from AND ud.discovered_at < :to AND c.created_at < :to)
            """, nativeQuery = true)
    Object[] conversionCounts(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            WITH per_session AS (
              SELECT vs.id, vs.user_id, EXTRACT(EPOCH FROM (vs.ended_at - vs.started_at)) / 60.0 AS duration_minutes,
                COUNT(vse.id) FILTER (WHERE vse.event_type = 'discovery') AS discoveries
              FROM visit_sessions vs LEFT JOIN visit_session_events vse ON vse.visit_session_id = vs.id
              WHERE vs.status = 'CLOSED' AND vs.ended_at IS NOT NULL AND vs.started_at >= :from AND vs.started_at < :to
                AND (:locationId IS NULL OR vs.location_id = :locationId)
              GROUP BY vs.id, vs.user_id, vs.started_at, vs.ended_at
            ) SELECT COUNT(DISTINCT user_id), AVG(duration_minutes), AVG(discoveries) FROM per_session
            """, nativeQuery = true)
    Object[] sessionQuality(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT ae.user_id), COUNT(ae.id),
              COUNT(DISTINCT CASE WHEN ae.event_type = 'quest_completed' THEN ae.user_id END),
              (SELECT COUNT(DISTINCT COALESCE(CAST(rv.user_id AS varchar), rv.session_id)) FROM referral_visits rv WHERE rv.visited_at >= :from AND rv.visited_at < :to)
            FROM analytics_events ae WHERE ae.created_at >= :from AND ae.created_at < :to
              AND (:locationId IS NULL OR ae.location_id = :locationId)
            """, nativeQuery = true)
    Object[] overviewCounts(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT CAST(ae.created_at AS date), COUNT(DISTINCT ae.user_id), COUNT(ae.id)
            FROM analytics_events ae WHERE ae.created_at >= :from AND ae.created_at < :to
              AND (:locationId IS NULL OR ae.location_id = :locationId)
            GROUP BY CAST(ae.created_at AS date) ORDER BY CAST(ae.created_at AS date)
            """, nativeQuery = true)
    List<Object[]> activityByDay(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT CAST(date_trunc('week', ae.created_at) AS date), COUNT(DISTINCT ae.user_id), COUNT(ae.id)
            FROM analytics_events ae WHERE ae.created_at >= :from AND ae.created_at < :to
              AND (:locationId IS NULL OR ae.location_id = :locationId)
            GROUP BY date_trunc('week', ae.created_at) ORDER BY date_trunc('week', ae.created_at)
            """, nativeQuery = true)
    List<Object[]> activityByWeek(@Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);

    /**
     * Hourly×weekday density for Stitch heatmap.
     * Columns: dayIndex Mon=0..Sun=6, slotIndex 0..23 (00:00..23:00), distinct users, event count.
     */
    @Query(value = """
            SELECT
              CAST((EXTRACT(DOW FROM ae.created_at) + 6) % 7 AS int) AS day_index,
              CAST(EXTRACT(HOUR FROM ae.created_at) AS int) AS slot_index,
              COUNT(DISTINCT ae.user_id),
              COUNT(ae.id)
            FROM analytics_events ae
            WHERE ae.created_at >= :from AND ae.created_at < :to
              AND (:locationId IS NULL OR ae.location_id = :locationId)
            GROUP BY 1, 2
            ORDER BY 1, 2
            """, nativeQuery = true)
    List<Object[]> activityHeatmapByDowAndSlot(
            @Param("locationId") UUID locationId, @Param("from") Instant from, @Param("to") Instant to);
}
