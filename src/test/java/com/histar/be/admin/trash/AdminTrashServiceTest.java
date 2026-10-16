package com.histar.be.admin.trash;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
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
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
    @Mock private ProfileRepository profiles;

    @Test
    void listBulkResolvesDeletedByNameWithoutPerItemLookup() {
        UUID deleterId = UUID.randomUUID();
        Quest first = Quest.builder().id(UUID.randomUUID()).title("First").build();
        first.setDeletedAt(java.time.Instant.parse("2026-10-15T02:00:00Z"));
        first.setDeletedBy(deleterId);
        Quest second = Quest.builder().id(UUID.randomUUID()).title("Second").build();
        second.setDeletedAt(java.time.Instant.parse("2026-10-15T01:00:00Z"));
        second.setDeletedBy(deleterId);
        when(discoveryPoints.findByDeletedAtIsNotNull()).thenReturn(List.of());
        when(artifacts.findByDeletedAtIsNotNull()).thenReturn(List.of());
        when(quests.findByDeletedAtIsNotNull()).thenReturn(List.of(first, second));
        when(profiles.findAllById(List.of(deleterId))).thenReturn(List.of(Profile.builder()
                .id(deleterId).displayName("Deletion Admin").email("admin@example.com").build()));

        var response = service.list(0, 20, null, null, null, "deletedAt,desc");

        assertThat(response.getItems()).extracting(AdminTrashItem::deletedByName)
                .containsExactly("Deletion Admin", "Deletion Admin");
        verify(profiles).findAllById(List.of(deleterId));
    }
    @InjectMocks private AdminTrashService service;

    @Test
    void softDeleteBlocksQuestWithActivePilot() {
        UUID questId = UUID.randomUUID();
        Quest quest = Quest.builder().id(questId).title("Active pilot").build();
        quest.setRowVersion(3L);
        when(quests.findIncludingDeletedById(questId)).thenReturn(Optional.of(quest));
        when(quests.findIncludingDeletedByIdForUpdate(questId)).thenReturn(Optional.of(quest));
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
        when(quests.findIncludingDeletedByIdForUpdate(questId)).thenReturn(Optional.of(quest));
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
        when(quests.findIncludingDeletedByIdForUpdate(questId)).thenReturn(Optional.of(quest));
        when(quests.saveAndFlush(quest)).thenReturn(quest);

        service.restoreQuest(questId, actorId, new AdminRestoreRequest(4L, "restore-quest-1"));

        assertThat(quest.getDeletedAt()).isNull();
        assertThat(quest.getDeletedBy()).isNull();
        assertThat(quest.getDeleteReason()).isNull();
        verify(audit).append(any());
    }

    @Test
    void restoreQuestReturnsParentDeletedConflictForItsRealParentQuest() {
        UUID questId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Quest quest = Quest.builder().id(questId).title("Child").unlockAfterQuestId(parentId).build();
        quest.setRowVersion(4L);
        quest.setDeletedAt(java.time.Instant.now());
        Quest parent = Quest.builder().id(parentId).title("Parent").build();
        parent.setDeletedAt(java.time.Instant.now());
        when(quests.findIncludingDeletedById(questId)).thenReturn(Optional.of(quest));
        when(quests.findIncludingDeletedByIdForUpdate(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.restoreQuest(
                questId, UUID.randomUUID(), new AdminRestoreRequest(4L, "restore-child")))
                .isInstanceOfSatisfying(NarrativeConflictException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARENT_DELETED));
        verify(audit, org.mockito.Mockito.never()).append(any());
    }

    @Test
    void restoreQuestLocksParentBeforeChildMutation() {
        UUID questId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Quest initial = Quest.builder().id(questId).unlockAfterQuestId(parentId).build();
        initial.setDeletedAt(java.time.Instant.now());
        initial.setRowVersion(2L);
        Quest parent = Quest.builder().id(parentId).build();
        Quest lockedChild = Quest.builder().id(questId).unlockAfterQuestId(parentId).build();
        lockedChild.setDeletedAt(initial.getDeletedAt());
        lockedChild.setRowVersion(2L);
        when(quests.findIncludingDeletedById(questId)).thenReturn(Optional.of(initial));
        when(quests.findIncludingDeletedByIdForUpdate(parentId)).thenReturn(Optional.of(parent));
        when(quests.findIncludingDeletedByIdForUpdate(questId)).thenReturn(Optional.of(lockedChild));
        when(quests.saveAndFlush(lockedChild)).thenReturn(lockedChild);

        service.restoreQuest(questId, UUID.randomUUID(), new AdminRestoreRequest(2L, "restore-lock"));

        var order = inOrder(quests);
        order.verify(quests).findIncludingDeletedByIdForUpdate(parentId);
        order.verify(quests).findIncludingDeletedByIdForUpdate(questId);
    }

    @Test
    void restoreQuestReturnsExplicitConflictWhenUniqueConstraintIsTaken() {
        UUID questId = UUID.randomUUID();
        Quest quest = Quest.builder().id(questId).title("Duplicate").build();
        quest.setRowVersion(2L);
        quest.setDeletedAt(java.time.Instant.now());
        when(quests.findIncludingDeletedById(questId)).thenReturn(Optional.of(quest));
        when(quests.findIncludingDeletedByIdForUpdate(questId)).thenReturn(Optional.of(quest));
        when(quests.saveAndFlush(quest)).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.restoreQuest(
                questId, UUID.randomUUID(), new AdminRestoreRequest(2L, "restore-duplicate")))
                .isInstanceOfSatisfying(NarrativeConflictException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESTORE_CONFLICT));
        verify(audit, org.mockito.Mockito.never()).append(any());
    }
}
