package com.histar.be.quest.dto;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.support.QuestDiscoveryProgress;
import java.util.UUID;

public record QuestResponse(
        UUID id,
        UUID locationId,
        String title,
        String description,
        String story,
        Integer pointsReward,
        Integer stepsTotal,
        Integer unlockLevel,
        String coverImage,
        String completionTrigger,
        Boolean requireOnsiteCheckin) {

    public static QuestResponse from(Quest quest) {
        boolean requireOnsite = Boolean.TRUE.equals(quest.getRequireOnsiteCheckin())
                || QuestDiscoveryProgress.requiresCheckinStep(quest);
        return new QuestResponse(
                quest.getId(),
                quest.getLocationId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStory(),
                quest.getPointsReward(),
                QuestDiscoveryProgress.stepsTotal(quest),
                1,
                quest.getCoverImage(),
                quest.getCompletionTrigger(),
                requireOnsite);
    }
}
