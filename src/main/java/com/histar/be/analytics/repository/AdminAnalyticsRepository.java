package com.histar.be.analytics.repository;

import com.histar.be.discovery.entity.DiscoveryPoint;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAnalyticsRepository extends JpaRepository<DiscoveryPoint, UUID> {

    @Query(
            value =
                    """
            SELECT dp.unlock_key, dp.name, COALESCE(COUNT(DISTINCT ud.user_id), 0)
            FROM discovery_points dp
            LEFT JOIN user_discoveries ud
              ON ud.discovery_key = dp.unlock_key AND ud.location_id = dp.location_id
            WHERE dp.location_id = :locationId
            GROUP BY dp.unlock_key, dp.name, dp.sort_order
            ORDER BY dp.sort_order
            """,
            nativeQuery = true)
    java.util.List<Object[]> poiUnlockCounts(@Param("locationId") UUID locationId);

    @Query(
            value =
                    """
            SELECT dp.unlock_key, dp.name, COALESCE(COUNT(vse.id), 0)
            FROM discovery_points dp
            LEFT JOIN visit_sessions vs ON vs.location_id = dp.location_id
            LEFT JOIN visit_session_events vse
              ON vse.visit_session_id = vs.id
              AND vse.event_type = 'discovery'
              AND vse.event_key = dp.unlock_key
            WHERE dp.location_id = :locationId
            GROUP BY dp.unlock_key, dp.name, dp.sort_order
            ORDER BY dp.sort_order
            """,
            nativeQuery = true)
    java.util.List<Object[]> poiVisitCounts(@Param("locationId") UUID locationId);

    @Query(
            value =
                    """
            SELECT COUNT(DISTINCT ud.user_id)
            FROM user_discoveries ud
            WHERE ud.location_id = :locationId
            """,
            nativeQuery = true)
    long countUsersWithDiscovery(@Param("locationId") UUID locationId);

    @Query(
            value =
                    """
            SELECT COUNT(DISTINCT c.user_id)
            FROM checkins c
            WHERE c.location_id = :locationId
            """,
            nativeQuery = true)
    long countUsersWithCheckin(@Param("locationId") UUID locationId);

    @Query(
            value =
                    """
            SELECT COUNT(DISTINCT ud.user_id)
            FROM user_discoveries ud
            INNER JOIN checkins c ON c.user_id = ud.user_id AND c.location_id = :locationId
            WHERE ud.location_id = :locationId
            AND ud.discovered_at <= c.created_at
            """,
            nativeQuery = true)
    long countUsersDiscoveryThenCheckin(@Param("locationId") UUID locationId);
}
