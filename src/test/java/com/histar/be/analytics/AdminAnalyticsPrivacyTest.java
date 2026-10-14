package com.histar.be.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
        when(analytics.poiUnlockCounts(locationId)).thenReturn(List.<Object[]>of(new Object[] {"poi:one", "One", 9L}));
        when(analytics.poiVisitCounts(locationId)).thenReturn(List.<Object[]>of(new Object[] {"poi:one", "One", 9L}));
        when(analytics.countUsersWithDiscovery(locationId)).thenReturn(9L);
        when(analytics.countUsersWithCheckin(locationId)).thenReturn(9L);
        when(analytics.countUsersDiscoveryThenCheckin(locationId)).thenReturn(9L);
        when(quests.findByLocationId(locationId)).thenReturn(List.of());
        when(sessions.findByLocationIdAndStatusAndEndedAtIsNotNull(locationId, "CLOSED"))
                .thenReturn(List.of());

        var result = service.overview(locationId);

        assertThat(result.poiUnlockRates()).singleElement().satisfies(item -> {
            assertThat(item.unlockCount()).isZero();
            assertThat(item.unlockRatePct()).isZero();
        });
        assertThat(result.journeyDropOff()).singleElement().satisfies(item -> {
            assertThat(item.sessionCount()).isZero();
            assertThat(item.dropOffPct()).isZero();
        });
        assertThat(result.poiHeatmap()).singleElement().satisfies(item -> {
            assertThat(item.visitCount()).isZero();
            assertThat(item.unlockCount()).isZero();
        });
        assertThat(result.onlineToOnsite().usersWithDiscovery()).isZero();
        assertThat(result.onlineToOnsite().usersWithCheckin()).isZero();
        assertThat(result.onlineToOnsite().usersDiscoveryThenCheckin()).isZero();
        assertThat(result.onlineToOnsite().conversionRatePct()).isZero();
    }
}
