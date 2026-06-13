package com.histar.be.quest.evaluator;

import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.service.impl.QuestStepProgressServiceImpl;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestCompletionEvaluator {

    private final UserDiscoveryRepository userDiscoveryRepository;

    public boolean isSatisfied(Quest quest, CheckinEventContext context) {
        if (quest == null || context == null) {
            return false;
        }
        UUID locationId = quest.getLocationId();
        if (quest.getStepDiscoveryKeys() != null && !quest.getStepDiscoveryKeys().isBlank()) {
            var steps = QuestStepProgressServiceImpl.parseSteps(quest.getStepDiscoveryKeys());
            if (!QuestStepProgressServiceImpl.allDiscoveryStepsDone(
                    context.userId(), locationId, steps, userDiscoveryRepository)) {
                return false;
            }
            return locationId != null && locationId.equals(context.locationId());
        }
        String trigger = quest.getCompletionTrigger() != null ? quest.getCompletionTrigger() : "checkin";
        if (!"checkin".equalsIgnoreCase(trigger)) {
            return false;
        }
        return locationId != null && locationId.equals(context.locationId());
    }
}
