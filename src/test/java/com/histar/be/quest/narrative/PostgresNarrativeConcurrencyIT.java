package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.entity.QuestRewardLedgerEntry;
import com.histar.be.quest.narrative.entity.UserNarrativeInvestigationProgress;
import com.histar.be.quest.narrative.model.NarrativeNode;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs only against an explicitly selected PostgreSQL environment. It proves that the profile-row
 * lock and durable reward ledger prevent duplicate XP when two narrative evidence actions race.
 */
@Tag("postgres-it")
@SpringBootTest
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_IT", matches = "true")
class PostgresNarrativeConcurrencyIT {

    @Autowired private NarrativeInvestigationService service;
    @Autowired private NarrativeInvestigationRepository investigations;
    @Autowired private UserNarrativeInvestigationProgressRepository progress;
    @Autowired private QuestRewardLedgerRepository rewards;
    @Autowired private ProfileRepository profiles;
    @Autowired private QuestRepository quests;
    @Autowired private LocationRepository locations;
    @Autowired private ObjectMapper objectMapper;

    private UUID userId;
    private UUID questId;
    private UUID investigationId;
    private UUID locationId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString();
        locationId = locations.save(Location.builder()
                .name("postgres-it-location-" + suffix)
                .city("Test")
                .latitude(10.0)
                .longitude(106.0)
                .createdAt(Instant.now())
                .build()).getId();
        questId = quests.save(Quest.builder()
                .locationId(locationId)
                .title("postgres-it-quest-" + suffix)
                .pointsReward(0)
                .requiredOrder(0)
                .completionTrigger("checkin")
                .stepsTotal(0)
                .build()).getId();
        userId = profiles.save(Profile.builder()
                .email("postgres-it-" + suffix + "@test.invalid")
                .displayName("Postgres integration test")
                .role("USER")
                .tier("FREE")
                .provider("local")
                .totalPoints(0)
                .level(1)
                .emailVerified(true)
                .createdAt(Instant.now())
                .build()).getId();
        NarrativeQuestGraph graph = new NarrativeQuestGraph(
                "evidence",
                List.of(new NarrativeNode("evidence", "OBSERVATION", List.of())),
                List.of("evidence"),
                List.of());
        investigationId = investigations.save(NarrativeInvestigation.builder()
                .questId(questId)
                .version(1)
                .enabled(true)
                .graphJson(objectMapper.writeValueAsString(graph))
                .reviewStatus("APPROVED")
                .approvedLocalesJson("[\"vi\",\"en\",\"ko\",\"zh-CN\"]")
                .reviewedBy("postgres-it")
                .reviewedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()).getId();
    }

    @AfterEach
    void cleanUp() {
        if (userId != null && questId != null) {
            rewards.deleteAll(rewards.findByUserIdAndQuestId(userId, questId));
        }
        if (userId != null && investigationId != null) {
            progress.findByUserIdAndInvestigationId(userId, investigationId).ifPresent(progress::delete);
        }
        if (investigationId != null) investigations.deleteById(investigationId);
        if (questId != null) quests.deleteById(questId);
        if (locationId != null) locations.deleteById(locationId);
        if (userId != null) profiles.deleteById(userId);
    }

    @Test
    void concurrentEvidenceActionAwardsExactlyOnceOnPostgres() throws Exception {
        int parallelRequests = 2;
        ExecutorService pool = Executors.newFixedThreadPool(parallelRequests);
        CountDownLatch ready = new CountDownLatch(parallelRequests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<NarrativeProgressResponse>> actions = List.of(
                pool.submit(() -> actWhenReleased(ready, start)),
                pool.submit(() -> actWhenReleased(ready, start)));

        ready.await();
        start.countDown();
        for (Future<NarrativeProgressResponse> action : actions) {
            assertThat(action.get()).isNotNull();
        }
        pool.shutdown();

        List<QuestRewardLedgerEntry> ledger = rewards.findByUserIdAndQuestId(userId, questId);
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).getRewardType()).isEqualTo("NARRATIVE_EVIDENCE");
        assertThat(profiles.findById(userId).orElseThrow().getTotalPoints()).isEqualTo(10);
        UserNarrativeInvestigationProgress stored = progress
                .findByUserIdAndInvestigationId(userId, investigationId).orElseThrow();
        assertThat(stored.getEvidenceCodesJson()).contains("evidence");
    }

    private NarrativeProgressResponse actWhenReleased(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return service.act(userId, questId,
                new NarrativeActionRequest(NarrativeActionType.COLLECT_EVIDENCE, "evidence", 1));
    }
}
