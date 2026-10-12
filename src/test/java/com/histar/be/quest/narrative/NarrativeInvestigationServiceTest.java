package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.entity.UserNarrativeInvestigationProgress;
import com.histar.be.profile.entity.Profile;
import com.histar.be.quest.minigame.UserGameResult;
import com.histar.be.quest.minigame.UserGameResultRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NarrativeInvestigationServiceTest {

    @Mock
    private NarrativeInvestigationRepository repository;
    @Mock private UserNarrativeInvestigationProgressRepository progressRepository;
    @Mock private NarrativeRewardService rewardService;
    @Mock private ProfileRepository profileRepository;
    @Mock private UserGameResultRepository minigameResults;

    @Test
    void returnsOnlyEnabledInvestigationAsTypedGraph() {
        UUID questId = UUID.randomUUID();
        NarrativeInvestigation investigation = NarrativeInvestigation.builder()
                .questId(questId)
                .version(1)
                .enabled(true)
                .graphJson("""
                        {"startNodeCode":"entry","nodes":[{"code":"entry","type":"BRIEFING","choices":[]}],
                        "evidenceNodeCodes":[],"outcomeNodeCodes":[]}
                        """)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .reviewStatus("APPROVED").approvedLocalesJson("[\"vi\"]").reviewedAt(Instant.now())
                .build();
        when(repository.findByQuestIdAndEnabledTrue(questId)).thenReturn(Optional.of(investigation));

        NarrativeInvestigationResponse response = new NarrativeInvestigationService(repository, new ObjectMapper(), progressRepository, rewardService, profileRepository)
                .forQuest(questId);

        assertThat(response.questId()).isEqualTo(questId);
        assertThat(response.graph().startNodeCode()).isEqualTo("entry");
        assertThat(response.graph().nodes()).singleElement().extracting("code").isEqualTo("entry");
    }

    @Test
    void doesNotExposeMissingOrDisabledInvestigation() {
        UUID questId = UUID.randomUUID();
        when(repository.findByQuestIdAndEnabledTrue(questId)).thenReturn(Optional.empty());

        NarrativeInvestigationService service = new NarrativeInvestigationService(repository, new ObjectMapper(), progressRepository, rewardService, profileRepository);

        assertThatThrownBy(() -> service.forQuest(questId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requires_linked_minigame_before_collecting_evidence_and_persists_progress() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID investigationId = UUID.randomUUID();
        UUID minigameId = UUID.fromString("a1000001-0000-4000-8000-000000000012");
        NarrativeInvestigation investigation = investigation(questId, investigationId, """
                {"startNodeCode":"start","nodes":[
                  {"code":"start","type":"OBSERVATION","choices":[{"code":"open","targetNodeCode":"evidence"}]},
                  {"code":"evidence","type":"EVIDENCE","choices":[],"linkedMinigameId":"a1000001-0000-4000-8000-000000000012","rewardXp":10}],
                  "evidenceNodeCodes":["evidence"],"outcomeNodeCodes":[]}
                """);
        UserNarrativeInvestigationProgress progress = progress(userId, investigationId, "[\"start\",\"evidence\"]", "[]");
        Profile profile = Profile.builder().id(userId).build();
        when(repository.findByQuestIdAndEnabledTrue(questId)).thenReturn(Optional.of(investigation));
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(progressRepository.findWithLockByUserIdAndInvestigationId(userId, investigationId)).thenReturn(Optional.of(progress));
        when(minigameResults.findByUserIdAndGameId(userId, minigameId)).thenReturn(Optional.empty());
        NarrativeInvestigationService service = service();

        assertThatThrownBy(() -> service.act(userId, questId,
                new NarrativeActionRequest(NarrativeActionType.COLLECT_EVIDENCE, "evidence", 1)))
                .hasMessageContaining("mini-game Soi nắp");

        UserGameResult result = new UserGameResult();
        result.setBestStars(1);
        when(minigameResults.findByUserIdAndGameId(userId, minigameId)).thenReturn(Optional.of(result));
        NarrativeProgressResponse response = service.act(userId, questId,
                new NarrativeActionRequest(NarrativeActionType.COLLECT_EVIDENCE, "evidence", 1));

        assertThat(response.progress().evidenceCodes()).containsExactly("evidence");
        org.mockito.Mockito.verify(rewardService).awardOnce(userId, questId, "NARRATIVE_EVIDENCE", "evidence", 10);
        org.mockito.Mockito.verify(progressRepository).save(progress);
    }

    @Test
    void rejects_selecting_an_outcome_that_is_not_reachable() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID investigationId = UUID.randomUUID();
        NarrativeInvestigation investigation = investigation(questId, investigationId, """
                {"startNodeCode":"start","nodes":[
                  {"code":"start","type":"OBSERVATION","choices":[]},
                  {"code":"outcome","type":"OUTCOME","minimumEvidence":0,"choices":[]}],
                  "evidenceNodeCodes":[],"outcomeNodeCodes":["outcome"]}
                """);
        when(repository.findByQuestIdAndEnabledTrue(questId)).thenReturn(Optional.of(investigation));
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(Profile.builder().id(userId).build()));
        when(progressRepository.findWithLockByUserIdAndInvestigationId(userId, investigationId)).thenReturn(Optional.of(progress(userId, investigationId, "[\"start\"]", "[]")));

        assertThatThrownBy(() -> service().act(userId, questId,
                new NarrativeActionRequest(NarrativeActionType.SELECT_OUTCOME, "outcome", 1)))
                .hasMessageContaining("Chuyển nhánh không hợp lệ");
    }

    @Test
    void records_only_declared_tour_observations_and_restores_them() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID investigationId = UUID.randomUUID();
        NarrativeInvestigation investigation = investigation(questId, investigationId, """
                {"startNodeCode":"tour","nodes":[
                  {"code":"tour","type":"TOUR_360_OBSERVATION","observationChecklist":["Thấy nắp hầm"],"choices":[]}],
                  "evidenceNodeCodes":[],"outcomeNodeCodes":[]}
                """);
        UserNarrativeInvestigationProgress progress = progress(userId, investigationId, "[\"tour\"]", "[]");
        when(repository.findByQuestIdAndEnabledTrue(questId)).thenReturn(Optional.of(investigation));
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(Profile.builder().id(userId).build()));
        when(progressRepository.findWithLockByUserIdAndInvestigationId(userId, investigationId)).thenReturn(Optional.of(progress));

        NarrativeProgressResponse response = service().act(userId, questId,
                new NarrativeActionRequest(NarrativeActionType.RECORD_OBSERVATION, "tour", "Thấy nắp hầm", 1));

        assertThat(response.progress().observationCodes()).containsExactly("Thấy nắp hầm");
        org.mockito.Mockito.verify(progressRepository).save(progress);
    }

    private NarrativeInvestigationService service() {
        return new NarrativeInvestigationService(repository, new ObjectMapper(), progressRepository, rewardService, profileRepository, minigameResults);
    }

    private NarrativeInvestigation investigation(UUID questId, UUID id, String graph) {
        return NarrativeInvestigation.builder().id(id).questId(questId).version(1).enabled(true).graphJson(graph)
                .createdAt(Instant.now()).updatedAt(Instant.now()).reviewStatus("APPROVED")
                .approvedLocalesJson("[\"vi\"]").reviewedAt(Instant.now()).build();
    }

    private UserNarrativeInvestigationProgress progress(UUID userId, UUID id, String visited, String evidence) {
        return UserNarrativeInvestigationProgress.builder().id(UUID.randomUUID()).userId(userId).investigationId(id)
                .stateJson("{\"version\":1,\"visitedNodeCodes\":[\"start\"],\"evidenceCodes\":[],\"selectedOutcome\":null}")
                .investigationVersion(1).visitedNodeCodesJson(visited).evidenceCodesJson(evidence).updatedAt(Instant.now()).build();
    }
}
