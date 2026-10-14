package com.histar.be.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentSoftDeleteVisibilityTest {

    @Autowired private DiscoveryPointRepository discoveryPoints;
    @Autowired private ArtifactRepository artifacts;
    @Autowired private QuestRepository quests;

    @Test
    void publicRepositoryReadsExcludeSoftDeletedContentButTrashReadsCanFindIt() {
        UUID locationId = UUID.randomUUID();
        DiscoveryPoint activePoint = discoveryPoints.saveAndFlush(DiscoveryPoint.builder()
                .locationId(locationId).name("Active point").unlockKey("point:active").sortOrder(1).build());
        DiscoveryPoint deletedPoint = discoveryPoints.saveAndFlush(DiscoveryPoint.builder()
                .locationId(locationId).name("Deleted point").unlockKey("point:deleted").sortOrder(2)
                .deletedAt(Instant.now()).build());

        Artifact activeArtifact = artifacts.saveAndFlush(Artifact.builder()
                .locationId(locationId).name("Active artifact").unlockKey("artifact:active").sortOrder(1).build());
        Artifact deletedArtifact = artifacts.saveAndFlush(Artifact.builder()
                .locationId(locationId).name("Deleted artifact").unlockKey("artifact:deleted").sortOrder(2)
                .deletedAt(Instant.now()).build());

        Quest activeQuest = quests.saveAndFlush(Quest.builder()
                .locationId(locationId).title("Active quest").requireOnsiteCheckin(false).build());
        Quest deletedQuest = quests.saveAndFlush(Quest.builder()
                .locationId(locationId).title("Deleted quest").requireOnsiteCheckin(false)
                .deletedAt(Instant.now()).build());

        assertThat(discoveryPoints.findByLocationIdOrderBySortOrder(locationId))
                .extracting(DiscoveryPoint::getId).containsExactly(activePoint.getId());
        assertThat(artifacts.findByLocationIdOrderBySortOrder(locationId))
                .extracting(Artifact::getId).containsExactly(activeArtifact.getId());
        assertThat(quests.findByLocationId(locationId))
                .extracting(Quest::getId).containsExactly(activeQuest.getId());

        assertThat(discoveryPoints.findById(deletedPoint.getId())).isEmpty();
        assertThat(artifacts.findById(deletedArtifact.getId())).isEmpty();
        assertThat(quests.findById(deletedQuest.getId())).isEmpty();
        assertThat(discoveryPoints.findIncludingDeletedById(deletedPoint.getId())).isPresent();
        assertThat(artifacts.findIncludingDeletedById(deletedArtifact.getId())).isPresent();
        assertThat(quests.findIncludingDeletedById(deletedQuest.getId())).isPresent();
    }
}
