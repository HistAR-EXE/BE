package com.histar.be.quest.narrative;

import java.util.UUID;

public record NarrativeDraftResponse(UUID questId, int version, String graphJson) {}
