package com.histar.be;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.analytics.repository.AnalyticsEventRepository;
import com.histar.be.analytics.service.impl.AnalyticsEventServiceImpl;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.support.CuChiTestFixture;
import com.histar.be.visit.repository.VisitSessionEventRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import com.histar.be.visit.service.VisitSessionService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsSessionConsistencyTest {

    @Autowired
    private CuChiTestFixture fixture;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private VisitSessionService visitSessionService;

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private QuestProgressService questProgressService;

    @Autowired
    private QuestCompletionService questCompletionService;

    @Autowired
    private CheckinRepository checkinRepository;

    @Autowired
    private AnalyticsEventRepository analyticsEventRepository;

    @Autowired
    private VisitSessionEventRepository visitSessionEventRepository;

    @Autowired
    private VisitSessionRepository visitSessionRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private UUID locationId;
    private UUID questId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        var data = fixture.loadValidFixture();
        locationId = data.locationId();
        questId = data.questId();

        Profile profile = profileRepository.save(Profile.builder()
                .email("consistency-" + UUID.randomUUID() + "@test.local")
                .passwordHash("hash")
                .displayName("Consistency User")
                .provider("local")
                .role("USER")
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    @Test
    void questAnalyticsAndSessionEvents_shareSessionAndPreserveOrder() {
        sessionId = visitSessionService.startSession(userId, locationId, "online");

        discoveryService.record(userId, CuChiTestFixture.KEY_A, locationId);
        visitSessionService.recordDiscoveryEvent(userId, CuChiTestFixture.KEY_A, "test", locationId);
        discoveryService.record(userId, CuChiTestFixture.KEY_B, locationId);
        visitSessionService.recordDiscoveryEvent(userId, CuChiTestFixture.KEY_B, "test", locationId);

        questProgressService.startQuest(userId, questId);

        Checkin savedCheckin = checkinRepository.save(Checkin.builder()
                .userId(userId)
                .locationId(locationId)
                .latitude(11.143)
                .longitude(106.461)
                .createdAt(Instant.now())
                .build());
        visitSessionService.recordCheckinEvent(userId, locationId, savedCheckin.getId());
        entityManager.flush();
        UUID activeSessionId = visitSessionRepository
                .findFirstByUserIdAndLocationIdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(
                        userId, locationId, "ACTIVE")
                .orElseThrow()
                .getId();
        assertThat(activeSessionId).isEqualTo(sessionId);
        assertThat(visitSessionEventRepository.findByVisitSessionIdOrderByCreatedAtAsc(sessionId))
                .extracting(e -> e.getEventType())
                .containsExactly("discovery", "discovery", "checkin");

        var completedQuests =
                questCompletionService.tryComplete(userId, locationId, CompletionTrigger.CHECKIN);
        assertThat(completedQuests).anyMatch(q -> questId.equals(q.questId()));

        var started = analyticsEventRepository
                .findFirstByUserIdAndEventTypeAndEventKey(
                        userId, AnalyticsEventServiceImpl.QUEST_STARTED, questId.toString())
                .orElseThrow();
        var completed = analyticsEventRepository
                .findFirstByUserIdAndEventTypeAndEventKey(
                        userId, AnalyticsEventServiceImpl.QUEST_COMPLETED, questId.toString())
                .orElseThrow();

        assertThat(started.getVisitSessionId()).isEqualTo(sessionId);
        assertThat(completed.getVisitSessionId()).isEqualTo(sessionId);
        assertThat(completed.getLocationId()).isEqualTo(locationId);
        assertThat(started.getUserId()).isEqualTo(userId);
        assertThat(started.getCreatedAt()).isBefore(completed.getCreatedAt());

        assertThat(completed.getMetadata()).isNotNull();
        assertThat(completed.getMetadata()).contains("\"questId\"");
        assertThat(completed.getMetadata()).contains("\"trigger\":\"CHECKIN\"");

        entityManager.flush();
        var sessionEvents = visitSessionEventRepository.findByVisitSessionIdOrderByCreatedAtAsc(sessionId);
        sessionEvents.forEach(e -> assertThat(e.getVisitSessionId()).isEqualTo(sessionId));

        assertThat(sessionEvents).hasSize(3);
        assertThat(sessionEvents.get(0).getEventType()).isEqualTo("discovery");
        assertThat(sessionEvents.get(1).getEventType()).isEqualTo("discovery");
        assertThat(sessionEvents.get(2).getEventType()).isEqualTo("checkin");
    }
}
