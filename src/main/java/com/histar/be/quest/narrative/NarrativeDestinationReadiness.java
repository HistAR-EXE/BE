package com.histar.be.quest.narrative;

import java.util.List;
import java.util.UUID;

/** Readiness of one real location/destination used by a narrative pilot. */
public record NarrativeDestinationReadiness(
        UUID destinationId,
        String destinationName,
        boolean required,
        boolean isReady,
        List<NarrativeReadinessItem> checks) {}
