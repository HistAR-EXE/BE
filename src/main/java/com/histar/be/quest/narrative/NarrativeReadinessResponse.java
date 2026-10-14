package com.histar.be.quest.narrative;

import java.util.List;
import java.util.UUID;

public record NarrativeReadinessResponse(UUID questId, Integer draftVersion, boolean ready,
        boolean globalFlagEnabled, List<NarrativeReadinessItem> items) {}
