package com.histar.be.quest.dto;

import com.histar.be.quest.entity.Quest;
import java.util.UUID;

public record QuestResponse(
        UUID id,
        UUID locationId,
        String title,
        String description,
        Integer pointsReward,
        Integer requiredOrder) {

    public static QuestResponse from(Quest quest) {
        return new QuestResponse(
                quest.getId(),
                quest.getLocationId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getPointsReward(),
                quest.getRequiredOrder());
    }
}
