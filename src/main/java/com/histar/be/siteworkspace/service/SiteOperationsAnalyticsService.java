package com.histar.be.siteworkspace.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.siteworkspace.dto.SiteOperationsSummaryResponse;
import com.histar.be.siteworkspace.dto.SiteOperationsSummaryResponse.StationCheckinCount;
import com.histar.be.siteworkspace.entity.SiteRole;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteOperationsAnalyticsService {

    static final Set<String> CONTENT_EVENT_TYPES = Set.of(
            "audio_played",
            "camera_opened",
            "game_completed",
            "pack_loaded",
            "portal_used",
            "video_played");

    private static final Set<SiteRole> READ_ROLES = Set.of(SiteRole.SITE_MANAGER, SiteRole.CONTENT_REVIEWER);

    private final SiteWorkspaceAccessService siteWorkspaceAccessService;
    private final LocationRepository locationRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public SiteOperationsSummaryResponse summary(UUID userId, String siteCode, Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BusinessRuleException("Khoảng thời gian không hợp lệ");
        }
        siteWorkspaceAccessService.requireAny(userId, siteCode, READ_ROLES);
        Location location = locationRepository.findBySiteCodeIgnoreCase(siteCode.trim())
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy site workspace"));
        UUID locationId = location.getId();
        return new SiteOperationsSummaryResponse(
                location.getSiteCode(),
                from,
                to,
                countVisits(locationId, from, to),
                checkinsByStation(locationId, from, to),
                countQuestCompletions(locationId, from, to),
                contentInteractions(locationId, from, to));
    }

    private long countVisits(UUID locationId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(v) FROM VisitSession v
                        WHERE v.locationId = :locationId
                          AND v.startedAt >= :from
                          AND v.startedAt < :to
                        """,
                        Long.class)
                .setParameter("locationId", locationId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    private List<StationCheckinCount> checkinsByStation(UUID locationId, Instant from, Instant to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createQuery(
                        """
                        SELECT c.stationCode, COUNT(c) FROM Checkin c
                        WHERE c.locationId = :locationId
                          AND c.createdAt >= :from
                          AND c.createdAt < :to
                          AND c.stationCode IS NOT NULL
                          AND c.stationCode <> ''
                        GROUP BY c.stationCode
                        ORDER BY c.stationCode
                        """)
                .setParameter("locationId", locationId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(row -> new StationCheckinCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    private long countQuestCompletions(UUID locationId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(p) FROM UserQuestProgress p
                        WHERE p.locationId = :locationId
                          AND LOWER(p.status) = 'completed'
                          AND p.completedAt >= :from
                          AND p.completedAt < :to
                        """,
                        Long.class)
                .setParameter("locationId", locationId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    private Map<String, Long> contentInteractions(UUID locationId, Instant from, Instant to) {
        long discoveries = entityManager.createQuery(
                        """
                        SELECT COUNT(d) FROM UserDiscovery d
                        WHERE d.locationId = :locationId
                          AND d.discoveredAt >= :from
                          AND d.discoveredAt < :to
                        """,
                        Long.class)
                .setParameter("locationId", locationId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        @SuppressWarnings("unchecked")
        List<Object[]> events = entityManager.createQuery(
                        """
                        SELECT e.eventType, COUNT(e) FROM PilotEvent e
                        WHERE e.occurredAt >= :from
                          AND e.occurredAt < :to
                          AND e.eventType IN :eventTypes
                          AND e.sessionId IN (
                              SELECT v.id FROM VisitSession v WHERE v.locationId = :locationId
                          )
                        GROUP BY e.eventType
                        ORDER BY e.eventType
                        """)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("eventTypes", CONTENT_EVENT_TYPES)
                .setParameter("locationId", locationId)
                .getResultList();
        Map<String, Long> interactions = new LinkedHashMap<>();
        if (discoveries > 0) {
            interactions.put("discovery", discoveries);
        }
        for (Object[] row : events) {
            String eventType = (String) row[0];
            if (CONTENT_EVENT_TYPES.contains(eventType)) {
                interactions.put(eventType, ((Number) row[1]).longValue());
            }
        }
        return Map.copyOf(interactions);
    }
}
