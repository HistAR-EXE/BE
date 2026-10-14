package com.histar.be.admin.trash;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.binding.repository.DiscoveryContentBindingRepository;
import com.histar.be.discovery.bridge.repository.ArtifactUnlockRequirementRepository;
import com.histar.be.discovery.bridge.repository.DiscoveryArtifactLinkRepository;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.lms.repository.AssignmentRepository;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.entity.Quest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTrashServiceTest {

    @Mock private QuestRepository quests;
    @Mock private NarrativeInvestigationRepository investigations;
    @Mock private DiscoveryPointRepository discoveryPoints;
    @Mock private ArtifactRepository artifacts;
    @Mock private QuestCampaignRepository campaigns;
    @Mock private UserQuestProgressRepository questProgress;
    @Mock private AssignmentRepository assignments;
    @Mock private StudyGroupRepository studyGroups;
    @Mock private DiscoveryContentBindingRepository bindings;
    @Mock private DiscoveryArtifactLinkRepository artifactLinks;
    @Mock private ArtifactUnlockRequirementRepository unlockRequirements;
    @Mock private UserDiscoveryRepository userDiscoveries;
    @Mock private UserArtifactRepository userArtifacts;
    @Mock private AdminAuditService audit;
    @InjectMocks private AdminTrashService service;

    @Test
    void softDeleteBlocksQuestWithActivePilot() {
        UUID questId = UUID.randomUUID();
        Quest quest = Quest.builder().id(questId).title("Active pilot").build();
        quest.setRowVersion(3L);
        when(quests.findIncludingDeletedById(questId)).thenReturn(Optional.of(quest));
        when(quests.findById(questId)).thenReturn(Optional.of(quest));
        when(investigations.findByQuestIdAndEnabledTrue(questId))
                .thenReturn(Optional.of(NarrativeInvestigation.builder().id(UUID.randomUUID()).questId(questId).enabled(true).build()));

        assertThatThrownBy(() -> service.softDeleteQuest(
                questId,
                UUID.randomUUID(),
                new AdminDeleteRequest("Lỗi kỹ thuật", null, 3L, "delete-quest-1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pilot active");
    }

    @Test
    void softDeleteMarksQuestHiddenAndAppendsAuditEvent() {
        UUID questId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Quest quest = Quest.builder().id(questId).title("Quest draft").build();
        quest.setRowVersion(3L);
        when(investigations.findByQuestIdAndEnabledTrue(questId)).thenReturn(Optional.empty());
        when(quests.findIncludingDeletedById(questId)).thenReturn(Optional.of(quest));
        when(quests.findById(questId)).thenReturn(Optional.of(quest));
        when(quests.saveAndFlush(quest)).thenReturn(quest);

        service.softDeleteQuest(questId, actorId, new AdminDeleteRequest("Sai nguồn", null, 3L, "delete-quest-2"));

        assertThat(quest.getDeletedAt()).isNotNull();
        assertThat(quest.getDeletedBy()).isEqualTo(actorId);
        assertThat(quest.getDeleteReason()).isEqualTo("Sai nguồn");
        verify(audit).append(any());
    }

    @Test
    void restoreQuestClearsOnlySoftDeleteFields() {
        UUID questId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Quest quest = Quest.builder().id(questId).title("Quest draft").build();
        quest.setRowVersion(4L);
        quest.setDeletedAt(java.time.Instant.now());
        quest.setDeletedBy(UUID.randomUUID());
        quest.setDeleteReason("Sai nguồn");
        when(quests.findIncludingDeletedById(questId)).thenReturn(Optional.of(quest));
        when(quests.saveAndFlush(quest)).thenReturn(quest);

        service.restoreQuest(questId, actorId, new AdminRestoreRequest(4L, "restore-quest-1"));

        assertThat(quest.getDeletedAt()).isNull();
        assertThat(quest.getDeletedBy()).isNull();
        assertThat(quest.getDeleteReason()).isNull();
        verify(audit).append(any());
    }
}
