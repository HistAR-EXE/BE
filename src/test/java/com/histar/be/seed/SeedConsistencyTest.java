package com.histar.be.seed;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.discovery.bridge.entity.ArtifactUnlockRequirement;
import com.histar.be.discovery.bridge.repository.ArtifactUnlockRequirementRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.minigame.QuestMinigame;
import com.histar.be.quest.minigame.QuestMinigameRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.support.CuChiTestFixture;
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
class SeedConsistencyTest {

    @Autowired
    private SeedConsistencyValidator validator;

    @Autowired
    private CuChiTestFixture fixture;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private QuestStepRepository questStepRepository;

    @Autowired
    private QuestMinigameRepository questMinigameRepository;

    @Autowired
    private ArtifactUnlockRequirementRepository artifactUnlockRequirementRepository;

    private UUID locationId;

    @BeforeEach
    void setUp() {
        locationId = fixture.loadValidFixture().locationId();
    }

    @Test
    void validateAll_passesOnValidFixture() {
        assertDoesNotThrow(() -> validator.validateAll());
    }

    @Test
    void validateAll_failsOnMissingQuestStepKey() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setStepDiscoveryKeys("scene:missing-key");
        questRepository.save(quest);

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());
        assertTrue(ex.getMessage().contains("scene:missing-key"));
    }

    @Test
    void validateAll_failsWhenVisibleQuestHasNoSteps() {
        Quest incomplete = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Incomplete quest")
                .description("Missing journey steps")
                .pointsReward(10)
                .stepsTotal(1)
                .build());

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains(incomplete.getId().toString()));
        assertTrue(ex.getMessage().contains("quest_steps"));
    }

    @Test
    void validateAll_failsWhenStepsTotalDiffersFromStoredSteps() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setStepsTotal(3);
        questRepository.save(quest);

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains("steps_total"));
    }

    @Test
    void validateAll_failsWhenMinigameStepReferencesMissingGame() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setStepsTotal(3);
        questRepository.save(quest);
        UUID missingGameId = UUID.randomUUID();
        questStepRepository.save(QuestStep.builder()
                .quest(quest)
                .stepOrder(3)
                .unlockKey("minigame:missing")
                .title("Missing minigame")
                .actionType("minigame")
                .minigameId(missingGameId)
                .build());

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains(missingGameId.toString()));
    }

    @Test
    void validateAll_failsWhenMinigameUsesUnsupportedEngine() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setStepsTotal(3);
        questRepository.save(quest);
        UUID gameId = UUID.randomUUID();
        QuestMinigame game = new QuestMinigame();
        game.setId(gameId);
        game.setTitle("Unsupported engine");
        game.setEngine("unknown_engine");
        game.setConfig("{\"levels\":[{\"id\":\"level-1\"}]}");
        questMinigameRepository.save(game);
        questStepRepository.save(QuestStep.builder()
                .quest(quest)
                .stepOrder(3)
                .unlockKey("minigame:unsupported-engine")
                .title("Unsupported engine")
                .actionType("minigame")
                .minigameId(gameId)
                .build());

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains("unknown_engine"));
    }

    @Test
    void validateAll_failsWhenPortalStepHasNoEra() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setStepsTotal(3);
        questRepository.save(quest);
        questStepRepository.save(QuestStep.builder()
                .quest(quest)
                .stepOrder(3)
                .unlockKey("era:missing")
                .title("Portal without era")
                .actionType("portal")
                .build());

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains("portal_era"));
    }

    @Test
    void validateAll_failsWhenDialogueHasNoPromptOrBackendKey() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setStepsTotal(3);
        questRepository.save(quest);
        questStepRepository.save(QuestStep.builder()
                .quest(quest)
                .stepOrder(3)
                .unlockKey("dialogue:not-configured")
                .title("Unconfigured dialogue")
                .actionType("dialogue")
                .build());

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains("chat_prompt"));
    }

    @Test
    void validateAll_failsWhenQuestDependsOnMissingQuest() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        UUID missingQuestId = UUID.randomUUID();
        quest.setUnlockAfterQuestId(missingQuestId);
        questRepository.save(quest);

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains(missingQuestId.toString()));
    }

    @Test
    void validateAll_failsWhenQuestDependsOnMissingDiscoveryKey() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setUnlockDiscoveryKeys("scene:not-configured");
        questRepository.save(quest);

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains("scene:not-configured"));
    }

    @Test
    void validateAll_failsWhenQuestStepHasNoBackendUnlockKey() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setStepsTotal(3);
        questRepository.save(quest);
        questStepRepository.save(QuestStep.builder()
                .quest(quest)
                .stepOrder(3)
                .unlockKey("scene:not-configured")
                .title("Unknown scene")
                .actionType("tour")
                .build());

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains("scene:not-configured"));
    }

    @Test
    void validateAll_failsWhenOnsiteQuestHasNoCheckinStep() {
        Quest quest = questRepository.findByLocationId(locationId).get(0);
        quest.setRequireOnsiteCheckin(true);
        questRepository.save(quest);

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.validateAll());

        assertTrue(ex.getMessage().contains("check-in"));
    }

    @Test
    void detectArtifactCycles_failsOnMutualDependency() {
        artifactUnlockRequirementRepository.save(ArtifactUnlockRequirement.builder()
                .locationId(locationId)
                .artifactUnlockKey("artifact:cycle-a")
                .requiredDiscoveryKey("artifact:cycle-b")
                .build());
        artifactUnlockRequirementRepository.save(ArtifactUnlockRequirement.builder()
                .locationId(locationId)
                .artifactUnlockKey("artifact:cycle-b")
                .requiredDiscoveryKey("artifact:cycle-a")
                .build());

        SeedConsistencyException ex =
                assertThrows(SeedConsistencyException.class, () -> validator.detectArtifactCycles());
        assertTrue(ex.getMessage().contains("artifact cycle detected"));
    }
}
