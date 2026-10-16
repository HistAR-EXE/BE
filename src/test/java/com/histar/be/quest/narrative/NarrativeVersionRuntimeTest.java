package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BaseException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.entity.NarrativeInvestigationVersion;
import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import com.histar.be.quest.narrative.entity.UserNarrativeInvestigationProgress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NarrativeVersionRuntimeTest {
    @Mock private NarrativeInvestigationRepository investigations;
    @Mock private NarrativeInvestigationVersionRepository versions;
    @Mock private UserNarrativeInvestigationProgressRepository progress;
    @Mock private NarrativeRewardService rewards;
    @Mock private ProfileRepository profiles;

    private NarrativeInvestigationService service;

    @BeforeEach
    void setUp() {
        service = new NarrativeInvestigationService(
                investigations, versions, new ObjectMapper(), progress, rewards, profiles, null);
    }

    @Test
    void existingProgressRemainsBoundToV1AfterV2IsPublished() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID investigationId = UUID.randomUUID();
        UUID v2Id = UUID.randomUUID();
        NarrativeInvestigation parent = parent(questId, investigationId, v2Id, NarrativeRolloutState.ACTIVE);
        UserNarrativeInvestigationProgress existing = UserNarrativeInvestigationProgress.builder()
                .id(UUID.randomUUID()).userId(userId).investigationId(investigationId).investigationVersion(1)
                .stateJson("{\"version\":1,\"visitedNodeCodes\":[\"v1-start\"],\"evidenceCodes\":[]}")
                .visitedNodeCodesJson("[\"v1-start\"]").evidenceCodesJson("[]")
                .observationCodesJson("[]").chatNodeCodesJson("[]").updatedAt(Instant.now()).build();
        when(investigations.findByQuestId(questId)).thenReturn(Optional.of(parent));
        when(progress.findByUserIdAndInvestigationId(userId, investigationId)).thenReturn(Optional.of(existing));
        when(versions.findByInvestigationIdAndVersion(investigationId, 1)).thenReturn(Optional.of(version(investigationId, 1, "v1-start")));

        NarrativeProgressResponse response = service.progress(userId, questId);

        assertThat(response.investigation().version()).isEqualTo(1);
        assertThat(response.investigation().graph().startNodeCode()).isEqualTo("v1-start");
        assertThat(response.progress().visitedNodeCodes()).containsExactly("v1-start");
    }

    @Test
    void missingHistoricalVersionNeverResetsProgress() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID investigationId = UUID.randomUUID();
        NarrativeInvestigation parent = parent(questId, investigationId, UUID.randomUUID(), NarrativeRolloutState.ACTIVE);
        UserNarrativeInvestigationProgress existing = UserNarrativeInvestigationProgress.builder()
                .id(UUID.randomUUID()).userId(userId).investigationId(investigationId).investigationVersion(1)
                .stateJson("{}").visitedNodeCodesJson("[]").evidenceCodesJson("[]").updatedAt(Instant.now()).build();
        when(investigations.findByQuestId(questId)).thenReturn(Optional.of(parent));
        when(progress.findByUserIdAndInvestigationId(userId, investigationId)).thenReturn(Optional.of(existing));
        when(versions.findByInvestigationIdAndVersion(investigationId, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.progress(userId, questId))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode().getCode())
                .isEqualTo("NARRATIVE_CANNOT_CONTINUE");
    }

    @Test
    void pausedPublishedVersionCanBeReadButBlocksActions() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID investigationId = UUID.randomUUID();
        UUID publishedId = UUID.randomUUID();
        NarrativeInvestigation parent = parent(questId, investigationId, publishedId, NarrativeRolloutState.PAUSED);
        when(investigations.findByQuestId(questId)).thenReturn(Optional.of(parent));
        when(versions.findById(publishedId)).thenReturn(Optional.of(version(investigationId, 1, "start")));
        when(progress.findByUserIdAndInvestigationId(userId, investigationId)).thenReturn(Optional.empty());
        when(profiles.findByIdForUpdate(userId)).thenReturn(Optional.of(Profile.builder().id(userId).build()));

        assertThat(service.progress(userId, questId).investigation().version()).isEqualTo(1);
        assertThatThrownBy(() -> service.act(userId, questId,
                new NarrativeActionRequest(NarrativeActionType.VISIT_NODE, "start", 1)))
                .hasMessageContaining("paused");
    }

    private NarrativeInvestigation parent(UUID questId, UUID id, UUID publishedId, NarrativeRolloutState state) {
        return NarrativeInvestigation.builder().id(id).questId(questId).version(2).enabled(state == NarrativeRolloutState.ACTIVE)
                .graphJson("{}").reviewStatus("APPROVED").approvedLocalesJson("[\"vi\"]")
                .publishedVersionId(publishedId).rolloutState(state).rowVersion(0L)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    private NarrativeInvestigationVersion version(UUID investigationId, int number, String start) {
        return NarrativeInvestigationVersion.builder().id(UUID.randomUUID()).investigationId(investigationId)
                .version(number).revision(0L).graphJson("{\"startNodeCode\":\"" + start
                        + "\",\"nodes\":[{\"code\":\"" + start
                        + "\",\"type\":\"BRIEFING\",\"choices\":[]}],\"evidenceNodeCodes\":[],\"outcomeNodeCodes\":[]}")
                .reviewStatus("APPROVED").approvedLocalesJson("[\"vi\"]").reviewedAt(Instant.now())
                .openDraft(false).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }
}
