package com.histar.be.analytics.service.impl;

import com.histar.be.analytics.entity.AnalyticsEvent;
import com.histar.be.analytics.repository.AnalyticsEventRepository;
import com.histar.be.analytics.service.AnalyticsEventService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    public static final String QUEST_STARTED = "QUEST_STARTED";
    public static final String QUEST_COMPLETED = "QUEST_COMPLETED";

    private final AnalyticsEventRepository analyticsEventRepository;

    @Override
    @Transactional
    public void recordQuestStarted(UUID userId, UUID locationId, UUID questId, UUID visitSessionId) {
        save(userId, locationId, visitSessionId, QUEST_STARTED, questId.toString());
    }

    @Override
    @Transactional
    public void recordQuestCompleted(UUID userId, UUID locationId, UUID questId, UUID visitSessionId) {
        recordQuestCompleted(userId, locationId, questId, visitSessionId, null);
    }

    @Override
    @Transactional
    public void recordQuestCompleted(
            UUID userId, UUID locationId, UUID questId, UUID visitSessionId, String metadataJson) {
        save(userId, locationId, visitSessionId, QUEST_COMPLETED, questId.toString(), metadataJson);
    }

    private void save(UUID userId, UUID locationId, UUID visitSessionId, String eventType, String eventKey) {
        save(userId, locationId, visitSessionId, eventType, eventKey, null);
    }

    private void save(
            UUID userId,
            UUID locationId,
            UUID visitSessionId,
            String eventType,
            String eventKey,
            String metadata) {
        analyticsEventRepository.save(AnalyticsEvent.builder()
                .userId(userId)
                .locationId(locationId)
                .visitSessionId(visitSessionId)
                .eventType(eventType)
                .eventKey(eventKey)
                .contentType("quest")
                .metadata(metadata)
                .createdAt(Instant.now())
                .build());
    }
}
