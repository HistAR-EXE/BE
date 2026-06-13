package com.histar.be.admin.dto;

import com.histar.be.quest.entity.Quest;
import java.util.UUID;

public record AdminQuestResponse(
        UUID id,
        UUID locationId,
        String title,
        String description,
        String story,
        Integer pointsReward,
        Integer requiredOrder) {

    public static AdminQuestResponse from(Quest quest) {
        return new AdminQuestResponse(
                quest.getId(),
                quest.getLocationId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStory(),
                quest.getPointsReward(),
                quest.getRequiredOrder());
    }
}
