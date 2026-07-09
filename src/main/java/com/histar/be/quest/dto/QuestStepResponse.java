package com.histar.be.quest.dto;

import com.histar.be.quest.entity.QuestStep;
import java.util.UUID;

public record QuestStepResponse(
        UUID id,
        Integer stepOrder,
        String unlockKey,
        String title,
        String objective,
        String description,
        String hint,
        String actionType,
        String actionLabel,
        Integer xpPartial,
        String chatPrompt,
        Integer portalEra,
        String previewImage
) {
    public static QuestStepResponse from(QuestStep entity) {
        return new QuestStepResponse(
                entity.getId(),
                entity.getStepOrder(),
                entity.getUnlockKey(),
                entity.getTitle(),
                entity.getObjective(),
                entity.getDescription(),
                entity.getHint(),
                entity.getActionType(),
                entity.getActionLabel(),
                entity.getXpPartial(),
                entity.getChatPrompt(),
                entity.getPortalEra(),
                entity.getPreviewImage()
        );
    }
}