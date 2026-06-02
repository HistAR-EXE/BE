package com.histar.be.quest.service;

import com.histar.be.quest.dto.QuestProgressResponse;
import com.histar.be.quest.dto.QuestResponse;
import java.util.List;
import java.util.UUID;

public interface QuestProgressService {

    List<QuestResponse> listByLocation(UUID locationId);

    List<QuestProgressResponse> listMyQuests(UUID userId, UUID locationId);

    QuestProgressResponse startQuest(UUID userId, UUID questId);

    QuestProgressResponse getProgress(UUID userId, UUID questId);
}
