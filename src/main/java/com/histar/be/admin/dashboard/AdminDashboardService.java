package com.histar.be.admin.dashboard;

import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.events.repository.PilotEventRepository;
import com.histar.be.admin.contentreport.ContentReportCaseRepository;
import com.histar.be.admin.contentreport.ContentReportStatus;
import com.histar.be.billing.service.AdminFinancialService;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import com.histar.be.quest.narrative.NarrativeInvestigationVersionRepository;
import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final NarrativeInvestigationRepository investigations;
    private final NarrativeInvestigationVersionRepository narrativeVersions;
    private final DiscoveryPointRepository discoveryPoints;
    private final ArtifactRepository artifacts;
    private final QuestRepository quests;
    private final PilotEventRepository pilotEvents;
    private final ContentReportCaseRepository contentReports;
    private final AdminFinancialService financials;

    @Transactional(readOnly = true)
    public AdminDashboardResponse get() {
        long trashCount = discoveryPoints.countByDeletedAtIsNotNull()
                + artifacts.countByDeletedAtIsNotNull()
                + quests.countByDeletedAtIsNotNull();
        long reports = pilotEvents.countByEventTypeAndOccurredAtGreaterThanEqual(
                "content_report", Instant.now().minus(30, ChronoUnit.DAYS));
        long openDrafts = narrativeVersions.countByOpenDraftTrue();
        long draftsPendingReview = narrativeVersions.countByOpenDraftTrueAndReviewStatus("DRAFT");
        long openReports = contentReports.countByStatusIn(
                List.of(ContentReportStatus.NEW, ContentReportStatus.IN_REVIEW));
        var finance = financials.overview(6);
        return new AdminDashboardResponse(
                investigations.countByRolloutState(NarrativeRolloutState.ACTIVE),
                openDrafts,
                draftsPendingReview,
                trashCount,
                reports,
                draftsPendingReview,
                openReports,
                finance.currentMonthRevenue(),
                finance.revenueMomPercent(),
                finance.estimatedMrr(),
                finance.estimatedArr(),
                finance.revenueTrend(),
                finance.comparisonStatus(),
                finance.mrrCoverage());
    }
}
