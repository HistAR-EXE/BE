package com.histar.be.analytics.service.impl;

import com.histar.be.analytics.AnalyticsBucket;
import com.histar.be.analytics.AnalyticsRange;
import com.histar.be.analytics.dto.*;
import com.histar.be.analytics.repository.AdminAnalyticsRepository;
import com.histar.be.analytics.service.AdminAnalyticsService;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.visit.repository.VisitSessionEventRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.sql.Date;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    // Retained constructor dependencies for binary/source compatibility; Phase 3 uses aggregate queries instead.
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final VisitSessionRepository visitSessionRepository;
    private final VisitSessionEventRepository visitSessionEventRepository;
    private final DiscoveryPointRepository discoveryPointRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewResponse overview(UUID locationId) {
        return overview(locationId, AnalyticsRange.D30);
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewResponse overview(UUID locationId, AnalyticsRange range) {
        Window window = window(range);
        List<PoiUnlockRateItem> poiRates = buildPoiUnlockRates(locationId, window);
        List<JourneyDropOffItem> dropOff = buildJourneyDropOff(locationId, window);
        List<QuestFunnelItem> questFunnel = buildQuestFunnel(locationId, window);
        OnlineOfflineConversion conversion = buildConversion(locationId, window);
        SessionQualityMetrics sessionQuality = buildSessionQuality(locationId, window);
        List<PoiHeatmapItem> heatmap = buildHeatmap(locationId, window, poiRates);
        Object[] counts = tuple(adminAnalyticsRepository.overviewCounts(locationId, window.from, window.to));
        long activeUsers = number(counts, 0);
        long events = number(counts, 1);
        long questCompleted = number(counts, 2);
        long referralUsers = number(counts, 3);
        boolean activitySuppressed = suppressed(activeUsers);
        boolean questSuppressed = suppressed(questCompleted);
        boolean referralSuppressed = suppressed(referralUsers);
        return new AdminAnalyticsOverviewResponse(
                locationId, poiRates, questFunnel, conversion, dropOff, heatmap, sessionQuality,
                questSuppressed ? null : questCompleted,
                dropOff.isEmpty() ? "Chưa có dữ liệu hành trình trong khoảng đã chọn."
                        : "Journey từ visit_sessions — POI drop-off theo người dùng distinct.",
                PRIVACY_THRESHOLD, questSuppressed,
                activitySuppressed ? null : activeUsers,
                activitySuppressed ? null : events,
                activitySuppressed,
                referralSuppressed ? null : referralUsers,
                referralSuppressed);
    }

    @Transactional(readOnly = true)
    public ActivitySeriesResponse activitySeries(UUID locationId, AnalyticsRange range, AnalyticsBucket bucket) {
        Window window = window(range);
        List<Object[]> rows = bucket == AnalyticsBucket.WEEK
                ? adminAnalyticsRepository.activityByWeek(locationId, window.from, window.to)
                : adminAnalyticsRepository.activityByDay(locationId, window.from, window.to);
        List<ActivitySeriesPoint> series = rows.stream().map(row -> {
            long users = number(row, 1);
            boolean hidden = suppressed(users);
            return new ActivitySeriesPoint(
                    toLocalDate(row[0]),
                    hidden ? null : users,
                    hidden ? null : number(row, 2),
                    hidden);
        }).toList();
        return new ActivitySeriesResponse(locationId, range, bucket, PRIVACY_THRESHOLD, series);
    }

    private static final int HEATMAP_SLOT_COUNT = 24; // 00:00 .. 23:00

    @Override
    @Transactional(readOnly = true)
    public ActivityHeatmapResponse activityHeatmap(UUID locationId, AnalyticsRange range) {
        Window window = window(range);
        List<String> dayLabels = List.of("T2", "T3", "T4", "T5", "T6", "T7", "CN");
        List<String> slotLabels = new ArrayList<>(HEATMAP_SLOT_COUNT);
        for (int hour = 0; hour < HEATMAP_SLOT_COUNT; hour++) {
            slotLabels.add(String.format("%02d:00", hour));
        }
        long[][] users = new long[7][HEATMAP_SLOT_COUNT];
        long[][] events = new long[7][HEATMAP_SLOT_COUNT];
        for (Object[] row : adminAnalyticsRepository.activityHeatmapByDowAndSlot(
                locationId, window.from, window.to)) {
            int day = (int) number(row, 0);
            int slot = (int) number(row, 1);
            if (day < 0 || day > 6 || slot < 0 || slot >= HEATMAP_SLOT_COUNT) {
                continue;
            }
            users[day][slot] = number(row, 2);
            events[day][slot] = number(row, 3);
        }
        long max = 0;
        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < HEATMAP_SLOT_COUNT; s++) {
                max = Math.max(max, users[d][s]);
            }
        }
        List<ActivityHeatmapCell> cells = new ArrayList<>();
        int daysWithData = 0;
        boolean[] dayHit = new boolean[7];
        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < HEATMAP_SLOT_COUNT; s++) {
                long u = users[d][s];
                if (u > 0) {
                    dayHit[d] = true;
                }
                boolean hidden = suppressed(u);
                double intensity = max == 0 || hidden ? 0 : u * 1.0 / max;
                cells.add(new ActivityHeatmapCell(
                        d,
                        s,
                        hidden ? null : u,
                        hidden ? null : events[d][s],
                        round(intensity),
                        hidden));
            }
        }
        for (boolean hit : dayHit) {
            if (hit) {
                daysWithData++;
            }
        }
        return new ActivityHeatmapResponse(
                locationId,
                range,
                PRIVACY_THRESHOLD,
                dayLabels,
                slotLabels,
                cells,
                peakSlotRange(users, 5, 11, slotLabels),
                peakSlotRange(users, 12, 18, slotLabels),
                daysWithData);
    }

    /** Peak 2-hour window label within [fromSlot, toSlot] (hour-of-day indices). */
    private String peakSlotRange(long[][] users, int fromSlot, int toSlot, List<String> slotLabels) {
        long best = -1;
        int bestSlot = fromSlot;
        for (int s = fromSlot; s <= toSlot; s++) {
            long sum = 0;
            for (int d = 0; d < 7; d++) {
                sum += users[d][s];
            }
            if (sum > best) {
                best = sum;
                bestSlot = s;
            }
        }
        if (best <= 0) {
            return "—";
        }
        int endHour = Math.min(bestSlot + 2, 24);
        return slotLabels.get(bestSlot) + " - " + String.format("%02d:00", endHour);
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        throw new IllegalArgumentException(
                "Unsupported bucket date type: " + (value == null ? "null" : value.getClass().getName()));
    }

    private List<PoiUnlockRateItem> buildPoiUnlockRates(UUID locationId, Window window) {
        List<Object[]> rows = adminAnalyticsRepository.poiUnlockCounts(locationId, window.from, window.to);
        long max = rows.stream().mapToLong(row -> number(row, 2)).max().orElse(0);
        return rows.stream().map(row -> {
            long count = number(row, 2);
            boolean hidden = suppressed(count);
            return new PoiUnlockRateItem((String) row[0], (String) row[1], hidden ? null : count,
                    hidden ? null : round(max == 0 ? 0 : count * 100.0 / max), hidden);
        }).toList();
    }

    private List<JourneyDropOffItem> buildJourneyDropOff(UUID locationId, Window window) {
        List<Object[]> rows = adminAnalyticsRepository.poiVisitCounts(locationId, window.from, window.to);
        long max = rows.stream().mapToLong(row -> number(row, 2)).max().orElse(0);
        return rows.stream().map(row -> {
            long count = number(row, 2);
            boolean hidden = suppressed(count);
            return new JourneyDropOffItem((String) row[1], (String) row[0], hidden ? null : count,
                    hidden ? null : round(max == 0 ? 0 : 100.0 - count * 100.0 / max), hidden);
        }).toList();
    }

    private List<QuestFunnelItem> buildQuestFunnel(UUID locationId, Window window) {
        return adminAnalyticsRepository.questFunnel(locationId, window.from, window.to).stream().map(row -> {
            long started = number(row, 2);
            long completed = number(row, 3);
            boolean hidden = suppressed(started) || suppressed(completed);
            return new QuestFunnelItem(uuid(row[0]), (String) row[1], hidden ? null : started,
                    hidden ? null : completed,
                    hidden ? null : round(started == 0 ? 0 : completed * 100.0 / started),
                    "checkin_with_discovery_steps", hidden);
        }).toList();
    }

    private OnlineOfflineConversion buildConversion(UUID locationId, Window window) {
        Object[] row = tuple(adminAnalyticsRepository.conversionCounts(locationId, window.from, window.to));
        long discovery = number(row, 0), checkin = number(row, 1), both = number(row, 2);
        boolean hidden = suppressed(discovery) || suppressed(checkin) || suppressed(both);
        return new OnlineOfflineConversion(hidden ? null : discovery, hidden ? null : checkin, hidden ? null : both,
                hidden ? null : round(discovery == 0 ? 0 : both * 100.0 / discovery), hidden);
    }

    private SessionQualityMetrics buildSessionQuality(UUID locationId, Window window) {
        Object[] row = tuple(adminAnalyticsRepository.sessionQuality(locationId, window.from, window.to));
        long users = number(row, 0);
        boolean hidden = suppressed(users);
        return new SessionQualityMetrics(hidden ? null : decimal(row, 1), hidden ? null : decimal(row, 2), hidden);
    }

    private List<PoiHeatmapItem> buildHeatmap(UUID locationId, Window window, List<PoiUnlockRateItem> unlocks) {
        Map<String, Object[]> visits = new HashMap<>();
        adminAnalyticsRepository.poiVisitCounts(locationId, window.from, window.to)
                .forEach(row -> visits.put((String) row[0], row));
        long max = visits.values().stream().mapToLong(row -> number(row, 2)).max().orElse(0);
        List<PoiHeatmapItem> result = new ArrayList<>();
        for (PoiUnlockRateItem unlock : unlocks) {
            long visitCount = visits.containsKey(unlock.unlockKey()) ? number(visits.get(unlock.unlockKey()), 2) : 0;
            boolean hidden = unlock.suppressed() || suppressed(visitCount);
            result.add(new PoiHeatmapItem(unlock.unlockKey(), unlock.name(), hidden ? null : visitCount,
                    hidden ? null : unlock.unlockCount(), null,
                    hidden ? null : max > 0 && visitCount < max * 0.35, hidden));
        }
        return result;
    }

    private Window window(AnalyticsRange range) {
        Instant to = Instant.now();
        return new Window(to.minus(range.days(), ChronoUnit.DAYS), to);
    }

    private boolean suppressed(long distinctUsers) { return distinctUsers < PRIVACY_THRESHOLD; }
    private Object[] tuple(Object[] row) {
        return row != null && row.length == 1 && row[0] instanceof Object[] nested ? nested : row;
    }
    private long number(Object[] row, int index) { return row == null || row.length <= index || row[index] == null ? 0 : ((Number) row[index]).longValue(); }
    private Double decimal(Object[] row, int index) { return row == null || row.length <= index || row[index] == null ? null : round(((Number) row[index]).doubleValue()); }
    private UUID uuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof byte[] bytes && bytes.length == 16) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(String.valueOf(value));
    }
    private Double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private record Window(Instant from, Instant to) {}
}
