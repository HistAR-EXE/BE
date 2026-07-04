package com.histar.be.admin.dto;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.support.QuestDiscoveryProgress;
import java.util.UUID;

public record AdminQuestResponse(
        UUID id,
        UUID locationId,
        String title,
        String description,
        String story,
        Integer pointsReward,
        Integer requiredOrder,
        String completionTrigger,
        Boolean requireOnsiteCheckin,
        Integer stepsTotal,
        String coverImage,
        String stepDiscoveryKeys) {

    public static AdminQuestResponse from(Quest quest) {
        return new AdminQuestResponse(
                quest.getId(),
                quest.getLocationId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStory(),
                quest.getPointsReward(),
                quest.getRequiredOrder(),
                quest.getCompletionTrigger(),
                Boolean.TRUE.equals(quest.getRequireOnsiteCheckin())
                        || QuestDiscoveryProgress.requiresCheckinStep(quest),
                quest.getStepsTotal(),
                quest.getCoverImage(),
                quest.getStepDiscoveryKeys());
    }
}
