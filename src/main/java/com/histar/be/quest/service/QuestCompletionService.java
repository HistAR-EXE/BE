package com.histar.be.quest.service;

import com.histar.be.gamification.dto.QuestCompletedDto;
import java.util.List;
import java.util.UUID;

public interface QuestCompletionService {

    List<QuestCompletedDto> tryComplete(UUID userId, UUID locationId, CompletionTrigger trigger);
}
