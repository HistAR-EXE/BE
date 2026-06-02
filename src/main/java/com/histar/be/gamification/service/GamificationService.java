package com.histar.be.gamification.service;

import com.histar.be.gamification.dto.CheckinResultDto;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.secret.dto.SecretStoryResponse;
import java.util.UUID;

public interface GamificationService {

    CheckinResultDto processCheckin(
            UUID userId, UUID locationId, double latitude, double longitude, String qrCode);

    CheckinResultDto processDemoCheckin(UUID userId, UUID locationId);

    QuestCompletedDto completeQuest(UUID userId, UUID questId);

    SecretStoryResponse getSecretStory(UUID userId, UUID locationId);
}
