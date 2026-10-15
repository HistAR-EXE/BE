package com.histar.be.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.analytics.repository.AdminAnalyticsRepository;
import com.histar.be.analytics.service.impl.AdminAnalyticsServiceImpl;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.visit.repository.VisitSessionEventRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsPhase3Test {
    @Mock AdminAnalyticsRepository analytics;
    @Mock QuestRepository quests;
    @Mock UserQuestProgressRepository progress;
    @Mock VisitSessionRepository sessions;
    @Mock VisitSessionEventRepository sessionEvents;
    @Mock DiscoveryPointRepository discoveryPoints;
    @InjectMocks AdminAnalyticsServiceImpl service;

    @Test
    void suppressesEverySensitiveValueWithNullIncludingReferral() {
        UUID locationId = UUID.randomUUID();
        when(analytics.poiUnlockCounts(eq(locationId), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {"poi:one", "One", 9L}));
        when(analytics.poiVisitCounts(eq(locationId), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {"poi:one", "One", 9L}));
        when(analytics.questFunnel(eq(locationId), any(), any())).thenReturn(List.<Object[]>of());
        when(analytics.conversionCounts(eq(locationId), any(), any())).thenReturn(new Object[] {9L, 9L, 9L});
        when(analytics.sessionQuality(eq(locationId), any(), any())).thenReturn(new Object[] {9L, 12.5, 2.0});
        when(analytics.overviewCounts(eq(locationId), any(), any())).thenReturn(new Object[] {9L, 9L, 9L, 9L});

        var result = service.overview(locationId, AnalyticsRange.D7);

        assertThat(result.poiUnlockRates()).singleElement().satisfies(item -> {
            assertThat(item.suppressed()).isTrue();
            assertThat(item.unlockCount()).isNull();
            assertThat(item.unlockRatePct()).isNull();
        });
        assertThat(result.onlineToOnsite().usersWithDiscovery()).isNull();
        assertThat(result.sessionQuality().avgDurationMinutes()).isNull();
        assertThat(result.activeUsers()).isNull();
        assertThat(result.referralUsers()).isNull();
        assertThat(result.referralSuppressed()).isTrue();
        verify(progress, never()).findAll();
        verify(sessions, never()).findByLocationIdAndStatusAndEndedAtIsNotNull(any(), any());
    }

    @Test
    void activitySeriesReturnsAggregateBuckets() {
        when(analytics.activityByDay(eq(null), any(), any())).thenReturn(List.of(
                new Object[] {java.sql.Date.valueOf(LocalDate.of(2026, 10, 13)), 12L, 20L},
                new Object[] {java.sql.Date.valueOf(LocalDate.of(2026, 10, 14)), 11L, 18L}));

        var result = service.activitySeries(null, AnalyticsRange.D7, AnalyticsBucket.DAY);

        assertThat(result.series()).hasSize(2);
        assertThat(result.series().get(0).activeUsers()).isEqualTo(12L);
        assertThat(result.series().get(0).suppressed()).isFalse();
    }

    @Test
    void activitySeriesAcceptsLocalDateBucketFromDriver() {
        when(analytics.activityByDay(eq(null), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {LocalDate.of(2026, 10, 13), 15L, 22L}));

        var result = service.activitySeries(null, AnalyticsRange.D7, AnalyticsBucket.DAY);

        assertThat(result.series()).singleElement()
                .extracting(point -> point.bucketStart(), point -> point.activeUsers())
                .containsExactly(LocalDate.of(2026, 10, 13), 15L);
    }

    @Test
    void questFunnelAcceptsDatabaseUuidBinaryRepresentation() {
        UUID questId = UUID.randomUUID();
        byte[] rawId = ByteBuffer.allocate(16)
                .putLong(questId.getMostSignificantBits())
                .putLong(questId.getLeastSignificantBits())
                .array();
        when(analytics.poiUnlockCounts(eq(null), any(), any())).thenReturn(List.of());
        when(analytics.poiVisitCounts(eq(null), any(), any())).thenReturn(List.of());
        when(analytics.questFunnel(eq(null), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {rawId, "Quest", 12L, 10L}));
        when(analytics.conversionCounts(eq(null), any(), any())).thenReturn(new Object[] {12L, 12L, 10L});
        when(analytics.sessionQuality(eq(null), any(), any())).thenReturn(new Object[] {12L, 15.0, 2.0});
        when(analytics.overviewCounts(eq(null), any(), any())).thenReturn(new Object[] {12L, 20L, 10L, 12L});

        var result = service.overview(null, AnalyticsRange.D30);

        assertThat(result.questFunnel()).singleElement()
                .extracting(item -> item.questId())
                .isEqualTo(questId);
    }

    @Test
    void overviewAcceptsNestedNativeAggregateTuples() {
        when(analytics.poiUnlockCounts(eq(null), any(), any())).thenReturn(List.of());
        when(analytics.poiVisitCounts(eq(null), any(), any())).thenReturn(List.of());
        when(analytics.questFunnel(eq(null), any(), any())).thenReturn(List.of());
        when(analytics.conversionCounts(eq(null), any(), any()))
                .thenReturn(new Object[] {new Object[] {12L, 11L, 10L}});
        when(analytics.sessionQuality(eq(null), any(), any()))
                .thenReturn(new Object[] {new Object[] {12L, 15.0, 2.0}});
        when(analytics.overviewCounts(eq(null), any(), any()))
                .thenReturn(new Object[] {new Object[] {12L, 20L, 10L, 12L}});

        var result = service.overview(null, AnalyticsRange.D30);

        assertThat(result.activeUsers()).isEqualTo(12L);
        assertThat(result.onlineToOnsite().usersWithDiscovery()).isEqualTo(12L);
        assertThat(result.sessionQuality().avgDurationMinutes()).isEqualTo(15.0);
    }
}
