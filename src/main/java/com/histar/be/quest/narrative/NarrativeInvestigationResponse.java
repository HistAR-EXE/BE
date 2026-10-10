package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.util.UUID;

public record NarrativeInvestigationResponse(UUID questId, int version, NarrativeQuestGraph graph) {}
