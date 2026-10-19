package com.histar.be.quest.service;

import com.histar.be.quest.dto.QuestProgressResponse;
import com.histar.be.quest.dto.QuestResponse;
import com.histar.be.quest.dto.QuestStartOutcome;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestProgressService {

    Page<QuestResponse> listByLocation(UUID locationId, Pageable pageable);

    Page<QuestProgressResponse> listMyQuests(UUID userId, UUID locationId, String status, Pageable pageable);

    QuestProgressResponse startQuest(UUID userId, UUID questId);

    /** Như {@link #startQuest} nhưng trả kèm các nhiệm vụ vừa hoàn thành ngay khi nhận. */
    QuestStartOutcome startQuestWithOutcome(UUID userId, UUID questId);

    QuestProgressResponse getProgress(UUID userId, UUID questId);
}
