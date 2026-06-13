package com.histar.be.seed;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.discovery.bridge.entity.ArtifactUnlockRequirement;
import com.histar.be.discovery.bridge.repository.ArtifactUnlockRequirementRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
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
