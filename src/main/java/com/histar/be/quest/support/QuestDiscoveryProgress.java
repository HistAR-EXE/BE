package com.histar.be.quest.support;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.service.impl.QuestStepProgressServiceImpl;
import java.util.List;
import java.util.UUID;

public final class QuestDiscoveryProgress {

    private QuestDiscoveryProgress() {}

    public record StepCounts(int currentStep, int stepsTotal) {}

    public static StepCounts compute(
            UUID userId, UUID locationId, Quest quest, String status, UserDiscoveryRepository repo) {
        List<String> steps = parseSteps(quest);
        int stepsTotal = steps.isEmpty() ? 1 : steps.size() + 1;
        if (QuestStatus.COMPLETED.equals(status)) {
            return new StepCounts(stepsTotal, stepsTotal);
        }
        UUID scopeLocationId = locationId != null ? locationId : quest.getLocationId();
        int discoveryStepsDone = countDone(userId, scopeLocationId, steps, repo);
        return new StepCounts(discoveryStepsDone, stepsTotal);
    }

    public static int countDone(UUID userId, UUID locationId, List<String> steps, UserDiscoveryRepository repo) {
        if (steps.isEmpty() || locationId == null) {
            return 0;
        }
        int done = 0;
        for (String key : steps) {
            if (repo.existsByUserIdAndLocationIdAndDiscoveryKey(userId, locationId, key)) {
                done++;
            }
        }
        return done;
    }

    public static List<String> parseSteps(Quest quest) {
        if (quest.getStepDiscoveryKeys() == null || quest.getStepDiscoveryKeys().isBlank()) {
            return List.of();
        }
        return QuestStepProgressServiceImpl.parseSteps(quest.getStepDiscoveryKeys());
    }
}
