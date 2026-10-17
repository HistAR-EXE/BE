package com.histar.be.discovery.service;

import com.histar.be.discovery.bridge.DiscoveryArtifactBridge;
import com.histar.be.discovery.dto.RecordDiscoveryResponse;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import com.histar.be.gamification.service.EngagementOutcomeService;
import com.histar.be.location.service.LocationUnlockService;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.visit.service.VisitSessionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All durable effects of a verified discovery succeed or roll back together. */
@Service @RequiredArgsConstructor
public class DiscoveryRewardService {
    private final DiscoveryService discoveries;
    private final ProfilePointsService points;
    private final DiscoveryArtifactBridge artifacts;
    private final QuestCompletionService quests;
    private final VisitSessionService visits;
    private final EngagementOutcomeService outcomes;
    private final LocationUnlockService locations;

    @Transactional
    public RecordDiscoveryResponse record(UUID userId, UUID locationId, String key, String source) {
        points.lockAccount(userId);
        boolean recorded = discoveries.record(userId, key, locationId);
        if (!recorded) return new RecordDiscoveryResponse(false, 0, List.of(), null, List.of());
        int xp = points.awardOnce(userId, "discovery:" + locationId + ":" + key,
                points.resolveXpForDiscoveryKey(key));
        List<UnlockedArtifactDto> unlocked = artifacts.unlockLinkedArtifacts(userId, key);
        for (var artifact : unlocked) {
            xp += points.awardOnce(userId, "artifact:" + artifact.id(), ProfilePointsService.XP_ARTIFACT_UNLOCK);
        }
        visits.recordDiscoveryEvent(userId, key, source, locationId);
        var completed = quests.tryComplete(userId, locationId, CompletionTrigger.DISCOVERY);
        return new RecordDiscoveryResponse(true, xp, unlocked,
                outcomes.resolveQuestProgress(userId, locationId, key, completed),
                locations.resolveNewlyUnlocked(userId, completed));
    }
}
