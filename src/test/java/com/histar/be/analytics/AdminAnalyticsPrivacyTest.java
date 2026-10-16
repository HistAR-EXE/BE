package com.histar.be.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.histar.be.analytics.repository.AdminAnalyticsRepository;
import com.histar.be.analytics.service.impl.AdminAnalyticsServiceImpl;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.visit.repository.VisitSessionEventRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsPrivacyTest {

    @Mock private AdminAnalyticsRepository analytics;
    @Mock private QuestRepository quests;
    @Mock private UserQuestProgressRepository progress;
    @Mock private VisitSessionRepository sessions;
    @Mock private VisitSessionEventRepository sessionEvents;
    @Mock private DiscoveryPointRepository discoveryPoints;
    @InjectMocks private AdminAnalyticsServiceImpl service;

    @Test
    void overviewSuppressesAggregateCellsBelowTen() {
        UUID locationId = UUID.randomUUID();
        when(analytics.poiUnlockCounts(eq(locationId), any(), any())).thenReturn(List.<Object[]>of(new Object[] {"poi:one", "One", 9L}));
        when(analytics.poiVisitCounts(eq(locationId), any(), any())).thenReturn(List.<Object[]>of(new Object[] {"poi:one", "One", 9L}));
        when(analytics.questFunnel(eq(locationId), any(), any())).thenReturn(List.of());
        when(analytics.conversionCounts(eq(locationId), any(), any())).thenReturn(new Object[] {9L, 9L, 9L});
        when(analytics.sessionQuality(eq(locationId), any(), any())).thenReturn(new Object[] {0L, null, null});
        when(analytics.overviewCounts(eq(locationId), any(), any())).thenReturn(new Object[] {9L, 9L, 9L, 9L});

        var result = service.overview(locationId);

        assertThat(result.poiUnlockRates()).singleElement().satisfies(item -> {
            assertThat(item.unlockCount()).isNull();
            assertThat(item.unlockRatePct()).isNull();
        });
        assertThat(result.journeyDropOff()).singleElement().satisfies(item -> {
            assertThat(item.sessionCount()).isNull();
            assertThat(item.dropOffPct()).isNull();
        });
        assertThat(result.poiHeatmap()).singleElement().satisfies(item -> {
            assertThat(item.visitCount()).isNull();
            assertThat(item.unlockCount()).isNull();
        });
        assertThat(result.onlineToOnsite().usersWithDiscovery()).isNull();
        assertThat(result.onlineToOnsite().usersWithCheckin()).isNull();
        assertThat(result.onlineToOnsite().usersDiscoveryThenCheckin()).isNull();
        assertThat(result.onlineToOnsite().conversionRatePct()).isNull();
    }
}
