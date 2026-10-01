package com.histar.be;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.analytics.service.impl.AnalyticsEventServiceImpl;
import com.histar.be.analytics.service.AdminAnalyticsService;
import com.histar.be.analytics.repository.AnalyticsEventRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.bridge.DiscoveryArtifactBridge;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.support.CuChiTestFixture;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.visit.entity.EndReason;
import com.histar.be.visit.repository.VisitSessionRepository;
import com.histar.be.visit.service.VisitSessionService;
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
class GoldenPathIntegrationTest {

    @Autowired
    private CuChiTestFixture fixture;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private VisitSessionService visitSessionService;

    @Autowired
    private VisitSessionRepository visitSessionRepository;

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private DiscoveryArtifactBridge discoveryArtifactBridge;

    @Autowired
    private QuestProgressService questProgressService;

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private UserQuestProgressRepository userQuestProgressRepository;

    @Autowired
    private UserArtifactRepository userArtifactRepository;

    @Autowired
    private AnalyticsEventRepository analyticsEventRepository;

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    private UUID userId;
    private UUID locationId;
    private UUID questId;
    private int questPointsReward;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        var data = fixture.loadValidFixture();
        locationId = data.locationId();
        questId = data.questId();
        questPointsReward = data.questPointsReward();

        Profile profile = profileRepository.save(Profile.builder()
                .email("golden-path-" + UUID.randomUUID() + "@test.local")
                .passwordHash("hash")
                .displayName("Golden Path User")
                .provider("local")
                .role("USER")
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    @Test
    void goldenPath_userActionToDashboardKpi() {
        sessionId = visitSessionService.startSession(userId, locationId, "online");

        discoveryService.record(userId, CuChiTestFixture.KEY_A, locationId);
        discoveryService.record(userId, CuChiTestFixture.KEY_B, locationId);
        discoveryArtifactBridge.unlockLinkedArtifacts(userId, CuChiTestFixture.KEY_A);

        questProgressService.startQuest(userId, questId);

        var checkin = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);
        assertThat(checkin.success()).isTrue();
        assertThat(checkin.questsCompleted()).contains(questId);

        visitSessionService.endSession(userId, sessionId, EndReason.USER_EXIT);

        var session = visitSessionRepository.findById(sessionId).orElseThrow();
        Instant now = Instant.now();
        session.setStartedAt(now.minusSeconds(300));
        session.setEndedAt(now);
        visitSessionRepository.save(session);

        var progress = userQuestProgressRepository.findByUserIdAndQuestId(userId, questId).orElseThrow();
        assertThat(progress.getStatus()).isEqualTo(QuestStatus.COMPLETED);

        var profile = profileRepository.findById(userId).orElseThrow();
        assertThat(profile.getTotalPoints()).isGreaterThanOrEqualTo(questPointsReward);

        assertThat(userArtifactRepository.existsByUserIdAndUnlockKeyAtLocation(
                        userId, locationId, CuChiTestFixture.ARTIFACT_KEY))
                .isTrue();

        assertThat(session.getStatus()).isEqualTo("CLOSED");
        assertThat(session.getEndedReason()).isEqualTo(EndReason.USER_EXIT.name());

        assertThat(analyticsEventRepository.existsByUserIdAndEventTypeAndEventKey(
                        userId, AnalyticsEventServiceImpl.QUEST_COMPLETED, questId.toString()))
                .isTrue();

        var overview = adminAnalyticsService.overview(locationId);
        assertThat(overview.sessionQuality().avgDurationMinutes()).isGreaterThan(0);
        assertThat(overview.questCompletedCount()).isEqualTo(1);
    }
}
