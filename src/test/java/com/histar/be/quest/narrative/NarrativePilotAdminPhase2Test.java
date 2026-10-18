package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.admin.audit.AdminAuditEvent;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.BaseException;
import com.histar.be.config.PilotFeatureProperties;
import com.histar.be.quest.entity.Quest;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.entity.NarrativeInvestigationVersion;
import com.histar.be.quest.narrative.entity.NarrativeRolloutIdempotency;
import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.stations.repository.StationContentBlockRepository;
import com.histar.be.stations.repository.StationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NarrativePilotAdminPhase2Test {
    @Mock NarrativeInvestigationRepository investigations;
    @Mock NarrativeInvestigationVersionRepository versions;
    @Mock NarrativeRolloutIdempotencyRepository idempotency;
    @Mock QuestRepository quests;
    @Mock LocationRepository locations;
    @Mock StationRepository stations;
    @Mock StationContentBlockRepository stationContentBlocks;
    @Mock AdminAuditService audit;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private NarrativePilotAdminService service;
    private NarrativeInvestigation parent;
    private NarrativeInvestigationVersion v1;

    @BeforeEach
    void setUp() {
        PilotFeatureProperties flags = new PilotFeatureProperties();
        flags.setNarrativeInvestigation(true);
        service = new NarrativePilotAdminService(investigations, versions, idempotency, quests, mapper,
                new NarrativeQuestGraphValidator(), flags, audit);
        UUID investigationId = UUID.randomUUID();
        parent = NarrativeInvestigation.builder().id(investigationId).questId(UUID.randomUUID()).version(1)
                .graphJson(graph("start")).enabled(false).reviewStatus("APPROVED").approvedLocalesJson("[\"vi\"]")
                .rolloutState(NarrativeRolloutState.PAUSED).rowVersion(0).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        v1 = version(1, false, "APPROVED", 0);
        parent.setPublishedVersionId(v1.getId());
    }

    @Test
    void rejectsCreatingASecondOpenDraft() {
        when(investigations.findForUpdateByQuestId(parent.getQuestId())).thenReturn(Optional.of(parent));
        when(versions.findByInvestigationIdAndOpenDraftTrue(parent.getId())).thenReturn(Optional.of(version(2, true, "DRAFT", 0)));

        assertThatThrownBy(() -> service.createDraft(parent.getQuestId(), UUID.randomUUID(),
                new NarrativeCreateDraftRequest(null, "Create next version")))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode().getCode())
                .isEqualTo("DRAFT_ALREADY_EXISTS");
    }

    @Test
    void staleDraftRevisionReturnsVersionConflict() {
        NarrativeInvestigationVersion draft = version(2, true, "APPROVED", 4);
        when(investigations.findForUpdateByQuestId(parent.getQuestId())).thenReturn(Optional.of(parent));
        when(versions.findByInvestigationIdAndVersion(parent.getId(), 2)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.patchDraft(parent.getQuestId(), 2, UUID.randomUUID(),
                new NarrativePatchDraftRequest(3, graph("changed"), "Update authored graph")))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode().getCode())
                .isEqualTo("VERSION_CONFLICT");
        verify(versions, never()).save(any());
    }

    @Test
    void editingApprovedDraftResetsOnlyThatDraftToDraft() {
        NarrativeInvestigationVersion draft = version(2, true, "APPROVED", 4);
        when(investigations.findForUpdateByQuestId(parent.getQuestId())).thenReturn(Optional.of(parent));
        when(versions.findByInvestigationIdAndVersion(parent.getId(), 2)).thenReturn(Optional.of(draft));
        when(versions.save(any())).thenAnswer(call -> call.getArgument(0));
        when(audit.append(any())).thenReturn(AdminAuditEvent.builder().id(UUID.randomUUID()).build());

        NarrativeVersionResponse updated = service.patchDraft(parent.getQuestId(), 2, UUID.randomUUID(),
                new NarrativePatchDraftRequest(4, graph("changed"), "Update authored graph"));

        assertThat(updated.revision()).isEqualTo(5);
        assertThat(updated.reviewStatus()).isEqualTo("DRAFT");
        assertThat(parent.getPublishedVersionId()).isEqualTo(v1.getId());
        assertThat(parent.getRolloutState()).isEqualTo(NarrativeRolloutState.PAUSED);
    }

    @Test
    void pausedV1CanResumeWhileApprovedV2CanPublish() {
        NarrativeInvestigationVersion v2 = version(2, true, "APPROVED", 2);
        stubLockedParent();
        when(versions.findById(v1.getId())).thenReturn(Optional.of(v1));
        when(versions.findByInvestigationIdAndVersion(parent.getId(), 2)).thenReturn(Optional.of(v2));
        when(versions.save(any())).thenAnswer(call -> call.getArgument(0));
        when(investigations.save(any())).thenAnswer(call -> call.getArgument(0));
        when(audit.append(any())).thenReturn(AdminAuditEvent.builder().id(UUID.randomUUID()).build());
        when(idempotency.findByInvestigationIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(idempotency.save(any())).thenAnswer(call -> call.getArgument(0));
        when(quests.findById(parent.getQuestId())).thenReturn(Optional.of(Quest.builder().id(parent.getQuestId()).title("Pilot").build()));
        when(versions.findByInvestigationIdOrderByVersionDesc(parent.getId())).thenReturn(java.util.List.of(v2, v1));

        service.resume(parent.getQuestId(), UUID.randomUUID(), "Resume current v1", "resume-key");
        assertThat(parent.getPublishedVersionId()).isEqualTo(v1.getId());
        assertThat(parent.getRolloutState()).isEqualTo(NarrativeRolloutState.ACTIVE);

        service.publishDraft(parent.getQuestId(), 2, UUID.randomUUID(), "Publish approved v2", "publish-key");
        assertThat(parent.getPublishedVersionId()).isEqualTo(v2.getId());
        assertThat(v2.isOpenDraft()).isFalse();
    }

    @Test
    void sameIdempotencyKeyWithDifferentRequestConflicts() {
        stubLockedParent();
        when(idempotency.findByInvestigationIdAndIdempotencyKey(parent.getId(), "same-key"))
                .thenReturn(Optional.of(NarrativeRolloutIdempotency.builder()
                        .investigationId(parent.getId()).idempotencyKey("same-key").action("RESUME")
                        .requestHash("different").responseJson("{}").createdAt(Instant.now()).build()));

        assertThatThrownBy(() -> service.resume(parent.getQuestId(), UUID.randomUUID(), "Resume current v1", "same-key"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode().getCode())
                .isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void resumeRequiresPausedState() {
        parent.setRolloutState(NarrativeRolloutState.ACTIVE);
        stubLockedParent();
        when(versions.findById(v1.getId())).thenReturn(Optional.of(v1));
        when(idempotency.findByInvestigationIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resume(parent.getQuestId(), UUID.randomUUID(), "Resume current v1", "resume-active"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("paused");
    }

    @Test
    void resumeRequiresFeatureFlagReadiness() {
        PilotFeatureProperties disabledFlags = new PilotFeatureProperties();
        disabledFlags.setNarrativeInvestigation(false);
        NarrativePilotAdminService disabledService = new NarrativePilotAdminService(
                investigations, versions, idempotency, quests, mapper,
                new NarrativeQuestGraphValidator(), disabledFlags, audit);
        stubLockedParent();
        when(versions.findById(v1.getId())).thenReturn(Optional.of(v1));
        when(idempotency.findByInvestigationIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disabledService.resume(
                parent.getQuestId(), UUID.randomUUID(), "Resume current v1", "resume-disabled"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("not ready");
    }

    @Test
    void rollbackRestoresTheRequestedPreviousPublishedVersionAndAuditsReason() {
        NarrativeInvestigationVersion v2 = version(2, false, "APPROVED", 0);
        stubLockedParent();
        parent.setRolloutState(NarrativeRolloutState.ACTIVE);
        parent.setPublishedVersionId(v2.getId());
        when(versions.findById(v2.getId())).thenReturn(Optional.of(v2));
        when(versions.findByInvestigationIdAndVersion(parent.getId(), 1)).thenReturn(Optional.of(v1));
        when(versions.findByInvestigationIdOrderByVersionDesc(parent.getId())).thenReturn(java.util.List.of(v2, v1));
        when(idempotency.findByInvestigationIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(idempotency.save(any())).thenAnswer(call -> call.getArgument(0));
        when(investigations.save(any())).thenAnswer(call -> call.getArgument(0));
        when(quests.findById(parent.getQuestId())).thenReturn(Optional.of(Quest.builder().id(parent.getQuestId()).title("Pilot").build()));

        service.rollback(parent.getQuestId(), 1, 0L, UUID.randomUUID(), "Lỗi kỹ thuật", "rollback-key");

        assertThat(parent.getPublishedVersionId()).isEqualTo(v1.getId());
        assertThat(parent.getRolloutState()).isEqualTo(NarrativeRolloutState.ACTIVE);
        verify(audit).append(org.mockito.ArgumentMatchers.argThat(command ->
                "ROLLED_BACK".equals(command.action()) && "Lỗi kỹ thuật".equals(command.reason())));
    }

    @Test
    void rollbackRequiresAnExplicitReason() {
        stubLockedParent();

        assertThatThrownBy(() -> service.rollback(parent.getQuestId(), 1, 0L, UUID.randomUUID(), " ", "rollback-key"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Rollback reason");
    }

    @Test
    void readinessFailsWhenQuestDestinationHasNoLocationOrMedia() {
        NarrativeInvestigationVersion draft = version(2, true, "APPROVED", 0);
        when(versions.findByInvestigationIdAndVersion(parent.getId(), 2)).thenReturn(Optional.of(draft));
        when(investigations.findByQuestId(parent.getQuestId())).thenReturn(Optional.of(parent));
        when(investigations.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(quests.findById(parent.getQuestId())).thenReturn(Optional.of(Quest.builder()
                .id(parent.getQuestId()).locationId(UUID.randomUUID()).build()));
        when(locations.findById(any())).thenReturn(Optional.empty());

        NarrativePilotAdminService destinationAwareService = new NarrativePilotAdminService(
                investigations, versions, idempotency, quests, mapper,
                new NarrativeQuestGraphValidator(), featureFlags(true), audit,
                locations, stations, stationContentBlocks);

        NarrativeReadinessResponse readiness = destinationAwareService.readiness(parent.getQuestId(), 2);

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.items()).extracting(NarrativeReadinessItem::code)
                .contains("DESTINATION_LOCATION", "MEDIA_READY");
    }

    @Test
    void pauseReasonValidationRequiresPresetAndCustomDetail() {
        assertThatThrownBy(() -> new NarrativePilotRolloutRequest(1, 0L, null, null, null).requiredPauseReason())
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("preset");
        assertThatThrownBy(() -> new NarrativePilotRolloutRequest(1, 0L, null, "Khác", "ngắn").requiredPauseReason())
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("10 characters");
        assertThat(new NarrativePilotRolloutRequest(1, 0L, null, "Lỗi kỹ thuật", null).requiredPauseReason())
                .isEqualTo("Lỗi kỹ thuật");
    }

    private void stubLockedParent() {
        when(investigations.findForUpdateByQuestId(parent.getQuestId())).thenReturn(Optional.of(parent));
    }

    private PilotFeatureProperties featureFlags(boolean enabled) {
        PilotFeatureProperties flags = new PilotFeatureProperties();
        flags.setNarrativeInvestigation(enabled);
        return flags;
    }

    private NarrativeInvestigationVersion version(int number, boolean open, String review, long revision) {
        return NarrativeInvestigationVersion.builder().id(UUID.randomUUID()).investigationId(parent == null ? UUID.randomUUID() : parent.getId())
                .version(number).graphJson(graph("start-v" + number)).reviewStatus(review)
                .approvedLocalesJson("APPROVED".equals(review) ? "[\"vi\"]" : "[]")
                .reviewedAt("APPROVED".equals(review) ? Instant.now() : null)
                .revision(revision).openDraft(open).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    private String graph(String start) {
        return "{\"startNodeCode\":\"" + start + "\",\"sources\":[{\"sourceId\":\"s\",\"version\":\"1\",\"locale\":\"vi\",\"citation\":\"citation\"}],"
                + "\"nodes\":[{\"code\":\"" + start + "\",\"type\":\"OBSERVATION\",\"choices\":[]}],\"evidenceNodeCodes\":[],\"outcomeNodeCodes\":[]}";
    }
}
