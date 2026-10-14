package com.histar.be.analytics.service.impl;

import com.histar.be.analytics.dto.AdminAnalyticsOverviewResponse;
import com.histar.be.analytics.dto.JourneyDropOffItem;
import com.histar.be.analytics.dto.OnlineOfflineConversion;
import com.histar.be.analytics.dto.PoiHeatmapItem;
import com.histar.be.analytics.dto.PoiUnlockRateItem;
import com.histar.be.analytics.dto.QuestFunnelItem;
import com.histar.be.analytics.dto.SessionQualityMetrics;
import com.histar.be.analytics.repository.AdminAnalyticsRepository;
import com.histar.be.analytics.service.AdminAnalyticsService;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.visit.entity.VisitSession;
import com.histar.be.visit.repository.VisitSessionEventRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final int PRIVACY_THRESHOLD = 10;

    private final AdminAnalyticsRepository adminAnalyticsRepository;
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final VisitSessionRepository visitSessionRepository;
    private final VisitSessionEventRepository visitSessionEventRepository;
    private final DiscoveryPointRepository discoveryPointRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewResponse overview(UUID locationId) {
        List<PoiUnlockRateItem> poiRates = buildPoiUnlockRates(locationId);
        List<QuestFunnelItem> questFunnel = buildQuestFunnel(locationId);
        OnlineOfflineConversion conversion = buildConversion(locationId);
        List<JourneyDropOffItem> journeyDropOff = buildJourneyDropOff(locationId);
        List<PoiHeatmapItem> heatmap = buildHeatmap(locationId, poiRates);
        SessionQualityMetrics sessionQuality = buildSessionQuality(locationId);
        long rawQuestCompletedCount = countQuestCompletedAtLocation(locationId);
        boolean questCompletedSuppressed = isSuppressed(rawQuestCompletedCount);
        long questCompletedCount = questCompletedSuppressed ? 0 : rawQuestCompletedCount;
        String journeyNote = journeyDropOff.isEmpty()
                ? "Chưa có visit session events — bắt đầu session khi vào Explore."
                : "Journey từ visit_sessions — POI drop-off theo session events.";
        return new AdminAnalyticsOverviewResponse(
                locationId,
                poiRates,
                questFunnel,
                conversion,
                journeyDropOff,
                heatmap,
                sessionQuality,
                questCompletedCount,
                journeyNote,
                PRIVACY_THRESHOLD,
                questCompletedSuppressed);
    }

    private List<PoiUnlockRateItem> buildPoiUnlockRates(UUID locationId) {
        List<Object[]> rows = adminAnalyticsRepository.poiUnlockCounts(locationId);
        long maxUnlock = rows.stream()
                .mapToLong(row -> ((Number) row[2]).longValue())
                .max()
                .orElse(0L);
        List<PoiUnlockRateItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            String unlockKey = (String) row[0];
            String name = (String) row[1];
            long count = ((Number) row[2]).longValue();
            boolean suppressed = isSuppressed(count);
            double rate = maxUnlock > 0 ? (count * 100.0 / maxUnlock) : 0.0;
            items.add(new PoiUnlockRateItem(
                    unlockKey, name, suppressed ? 0 : count, suppressed ? 0 : Math.round(rate * 10) / 10.0, suppressed));
        }
        return items;
    }

    private List<JourneyDropOffItem> buildJourneyDropOff(UUID locationId) {
        List<Object[]> visitRows = adminAnalyticsRepository.poiVisitCounts(locationId);
        long maxVisits = visitRows.stream()
                .mapToLong(row -> ((Number) row[2]).longValue())
                .max()
                .orElse(0L);
        List<JourneyDropOffItem> items = new ArrayList<>();
        for (Object[] row : visitRows) {
            String unlockKey = (String) row[0];
            String name = (String) row[1];
            long visits = ((Number) row[2]).longValue();
            if (visits == 0) continue;
            boolean suppressed = isSuppressed(visits);
            double dropOff = maxVisits > 0 ? (100.0 - visits * 100.0 / maxVisits) : 0.0;
            items.add(new JourneyDropOffItem(
                    name, unlockKey, suppressed ? 0 : visits,
                    suppressed ? 0 : Math.round(dropOff * 10) / 10.0, suppressed));
        }
        return items;
    }

    private SessionQualityMetrics buildSessionQuality(UUID locationId) {
        List<VisitSession> closed =
                visitSessionRepository.findByLocationIdAndStatusAndEndedAtIsNotNull(locationId, "CLOSED");
        if (closed.isEmpty()) {
            return new SessionQualityMetrics(0, 0, false);
        }
        if (closed.size() < PRIVACY_THRESHOLD) {
            return new SessionQualityMetrics(0, 0, true);
        }
        double totalMinutes = 0;
        double totalDiscoveries = 0;
        for (VisitSession session : closed) {
            if (session.getStartedAt() != null && session.getEndedAt() != null) {
                long seconds = session.getEndedAt().getEpochSecond() - session.getStartedAt().getEpochSecond();
                totalMinutes += Math.max(0, seconds) / 60.0;
            }
            long discoveries = visitSessionEventRepository.findByVisitSessionIdOrderByCreatedAtAsc(session.getId())
                    .stream()
                    .filter(e -> "discovery".equals(e.getEventType()))
                    .count();
            totalDiscoveries += discoveries;
        }
        int count = closed.size();
        double avgDuration = Math.round((totalMinutes / count) * 10) / 10.0;
        double avgDiscoveries = Math.round((totalDiscoveries / count) * 10) / 10.0;
        return new SessionQualityMetrics(avgDuration, avgDiscoveries, false);
    }

    private List<PoiHeatmapItem> buildHeatmap(UUID locationId, List<PoiUnlockRateItem> poiRates) {
        Map<String, Long> visits = new HashMap<>();
        for (Object[] row : adminAnalyticsRepository.poiVisitCounts(locationId)) {
            visits.put((String) row[0], ((Number) row[2]).longValue());
        }
        long maxVisit = visits.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<PoiHeatmapItem> items = new ArrayList<>();
        for (PoiUnlockRateItem poi : poiRates) {
            long visitCount = visits.getOrDefault(poi.unlockKey(), 0L);
            boolean suppressed = isSuppressed(visitCount) || poi.suppressed();
            boolean dropOff = maxVisit > 0 && visitCount > 0 && visitCount < maxVisit * 0.35;
            items.add(new PoiHeatmapItem(
                    poi.unlockKey(),
                    poi.name(),
                    suppressed ? 0 : visitCount,
                    poi.unlockCount(),
                    null,
                    suppressed ? false : dropOff,
                    suppressed));
        }
        items.sort((a, b) -> Long.compare(b.visitCount(), a.visitCount()));
        return items;
    }

    private List<QuestFunnelItem> buildQuestFunnel(UUID locationId) {
        List<Quest> quests = questRepository.findByLocationId(locationId);
        List<QuestFunnelItem> items = new ArrayList<>();
        for (Quest quest : quests) {
            List<UserQuestProgress> progresses = userQuestProgressRepository.findAll().stream()
                    .filter(p -> quest.getId().equals(p.getQuestId()))
                    .toList();
            long started = progresses.stream()
                    .filter(p -> QuestStatus.IN_PROGRESS.equals(p.getStatus())
                            || QuestStatus.COMPLETED.equals(p.getStatus()))
                    .count();
            long completed = progresses.stream()
                    .filter(p -> QuestStatus.COMPLETED.equals(p.getStatus()))
                    .count();
            boolean suppressed = isSuppressed(started) || isSuppressed(completed);
            double rate = started > 0 ? (completed * 100.0 / started) : 0.0;
            items.add(new QuestFunnelItem(
                    quest.getId(),
                    quest.getTitle(),
                    suppressed ? 0 : started,
                    suppressed ? 0 : completed,
                    suppressed ? 0 : Math.round(rate * 10) / 10.0,
                    "checkin_with_discovery_steps",
                    suppressed));
        }
        return items;
    }

    private OnlineOfflineConversion buildConversion(UUID locationId) {
        long withDiscovery = adminAnalyticsRepository.countUsersWithDiscovery(locationId);
        long withCheckin = adminAnalyticsRepository.countUsersWithCheckin(locationId);
        long both = adminAnalyticsRepository.countUsersDiscoveryThenCheckin(locationId);
        boolean suppressed = isSuppressed(withDiscovery) || isSuppressed(withCheckin) || isSuppressed(both);
        double rate = withDiscovery > 0 ? (both * 100.0 / withDiscovery) : 0.0;
        return new OnlineOfflineConversion(
                suppressed ? 0 : withDiscovery,
                suppressed ? 0 : withCheckin,
                suppressed ? 0 : both,
                suppressed ? 0 : Math.round(rate * 10) / 10.0,
                suppressed);
    }

    private long countQuestCompletedAtLocation(UUID locationId) {
        List<UUID> questIds = questRepository.findByLocationId(locationId).stream()
                .map(Quest::getId)
                .toList();
        if (questIds.isEmpty()) {
            return 0;
        }
        return userQuestProgressRepository.findAll().stream()
                .filter(p -> QuestStatus.COMPLETED.equals(p.getStatus()))
                .filter(p -> questIds.contains(p.getQuestId()))
                .count();
    }

    private boolean isSuppressed(long count) {
        return count > 0 && count < PRIVACY_THRESHOLD;
    }
}
