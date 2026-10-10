package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
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
}
