package com.histar.be.quest.narrative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NarrativeInvestigationService {
    private final NarrativeInvestigationRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public NarrativeInvestigationResponse forQuest(UUID questId) {
        NarrativeInvestigation investigation = repository.findByQuestIdAndEnabledTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        try {
            return new NarrativeInvestigationResponse(
                    investigation.getQuestId(), investigation.getVersion(),
                    objectMapper.readValue(investigation.getGraphJson(), NarrativeQuestGraph.class));
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Narrative investigation graph không đọc được");
        }
    }
}
