package com.histar.be.admin.dashboard;

import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.events.repository.PilotEventRepository;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import com.histar.be.quest.narrative.NarrativeInvestigationVersionRepository;
import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    @Transactional(readOnly = true)
    public AdminDashboardResponse get() {
        long trashCount = discoveryPoints.countByDeletedAtIsNotNull()
                + artifacts.countByDeletedAtIsNotNull()
                + quests.countByDeletedAtIsNotNull();
        long reports = pilotEvents.countByEventTypeAndOccurredAtGreaterThanEqual(
                "content_report", Instant.now().minus(30, ChronoUnit.DAYS));
        return new AdminDashboardResponse(
                investigations.countByRolloutState(NarrativeRolloutState.ACTIVE),
                narrativeVersions.countByOpenDraftTrueAndReviewStatus("DRAFT"),
                0,
                trashCount,
                reports);
    }
}
