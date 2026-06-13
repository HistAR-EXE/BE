package com.histar.be.analytics.service;

import java.util.UUID;

public interface AnalyticsEventService {

    void recordQuestStarted(UUID userId, UUID locationId, UUID questId, UUID visitSessionId);

    void recordQuestCompleted(UUID userId, UUID locationId, UUID questId, UUID visitSessionId);

    void recordQuestCompleted(
            UUID userId, UUID locationId, UUID questId, UUID visitSessionId, String metadataJson);
}
