package com.histar.be.admin.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.histar.be.admin.contentreport.ContentReportCaseRepository;
import com.histar.be.admin.contentreport.ContentReportStatus;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.billing.dto.AdminBillingOverviewResponse;
import com.histar.be.billing.dto.MrrCoverage;
import com.histar.be.billing.dto.RevenueTrendPoint;
import com.histar.be.billing.service.AdminFinancialService;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.events.repository.PilotEventRepository;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import com.histar.be.quest.narrative.NarrativeInvestigationVersionRepository;
import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import com.histar.be.quest.repository.QuestRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {
    @Mock NarrativeInvestigationRepository investigations;
    @Mock NarrativeInvestigationVersionRepository narrativeVersions;
    @Mock DiscoveryPointRepository discoveryPoints;
    @Mock ArtifactRepository artifacts;
    @Mock QuestRepository quests;
    @Mock PilotEventRepository pilotEvents;
    @Mock ContentReportCaseRepository contentReports;
    @Mock AdminFinancialService financials;
    @InjectMocks AdminDashboardService service;

    @Test
    void returnsAdditiveOperationalAndFinancialFieldsWithoutPlaceholderZeros() {
        when(investigations.countByRolloutState(NarrativeRolloutState.ACTIVE)).thenReturn(2L);
        when(narrativeVersions.countByOpenDraftTrue()).thenReturn(5L);
        when(narrativeVersions.countByOpenDraftTrueAndReviewStatus("DRAFT")).thenReturn(3L);
        when(discoveryPoints.countByDeletedAtIsNotNull()).thenReturn(1L);
        when(artifacts.countByDeletedAtIsNotNull()).thenReturn(2L);
        when(quests.countByDeletedAtIsNotNull()).thenReturn(3L);
        when(contentReports.countByStatusIn(List.of(ContentReportStatus.NEW, ContentReportStatus.IN_REVIEW)))
                .thenReturn(4L);
        when(financials.overview(6)).thenReturn(new AdminBillingOverviewResponse(
                100L, 25.0, "OK", null, "INSUFFICIENT_HISTORY", 50L, 600L,
                List.of(new RevenueTrendPoint("2026-10", 40L, 60L, 100L)),
                new MrrCoverage(2, 1, 66.7),
                List.of()));

        var result = service.get();

        assertThat(result.activePilots()).isEqualTo(2);
        assertThat(result.draftCount()).isEqualTo(5);
        assertThat(result.draftPendingReview()).isEqualTo(3);
        assertThat(result.reviewPendingCount()).isEqualTo(3);
        assertThat(result.openContentReports()).isEqualTo(4);
        assertThat(result.trashCount()).isEqualTo(6);
        assertThat(result.currentMonthRevenue()).isEqualTo(100L);
        assertThat(result.estimatedArr()).isEqualTo(600L);
        assertThat(result.revenueTrend()).hasSize(1);
    }
}
