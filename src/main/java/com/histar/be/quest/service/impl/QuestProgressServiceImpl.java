package com.histar.be.quest.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.quest.dto.QuestProgressResponse;
import com.histar.be.quest.dto.QuestResponse;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.quest.service.QuestService;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestProgressServiceImpl implements QuestProgressService {

    private final QuestService questService;
    private final UserQuestProgressRepository userQuestProgressRepository;

    @Override
    public List<QuestResponse> listByLocation(UUID locationId) {
        return questService.findByLocationId(locationId).stream()
                .map(QuestResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestProgressResponse> listMyQuests(UUID userId, UUID locationId) {
        return questService.findByLocationId(locationId).stream()
                .map(quest -> toProgress(userId, quest))
                .toList();
    }

    @Override
    @Transactional
    public QuestProgressResponse startQuest(UUID userId, UUID questId) {
        Quest quest = questService.findById(questId);
        UserQuestProgress progress = userQuestProgressRepository
                .findByUserIdAndQuestId(userId, questId)
                .orElseGet(() -> UserQuestProgress.builder()
                        .userId(userId)
                        .questId(questId)
                        .status(QuestStatus.NOT_STARTED)
                        .build());

        if (QuestStatus.COMPLETED.equals(progress.getStatus())) {
            throw new BusinessRuleException("Quest đã hoàn thành");
        }
        if (QuestStatus.IN_PROGRESS.equals(progress.getStatus())) {
            return toProgress(userId, quest);
        }
        if (!QuestStatus.NOT_STARTED.equals(progress.getStatus())) {
            throw new BusinessRuleException("Không thể bắt đầu quest từ trạng thái hiện tại");
        }

        progress.setStatus(QuestStatus.IN_PROGRESS);
        progress.setStartedAt(Instant.now());
        userQuestProgressRepository.save(progress);
        return toProgress(userId, quest);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestProgressResponse getProgress(UUID userId, UUID questId) {
        Quest quest = questService.findById(questId);
        return toProgress(userId, quest);
    }

    private QuestProgressResponse toProgress(UUID userId, Quest quest) {
        String status = userQuestProgressRepository
                .findByUserIdAndQuestId(userId, quest.getId())
                .map(UserQuestProgress::getStatus)
                .orElse(QuestStatus.NOT_STARTED);
        UserQuestProgress progress = userQuestProgressRepository
                .findByUserIdAndQuestId(userId, quest.getId())
                .orElse(null);
        return new QuestProgressResponse(
                quest.getId(),
                quest.getLocationId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getPointsReward(),
                status,
                progress != null ? progress.getStartedAt() : null,
                progress != null ? progress.getCompletedAt() : null);
    }
}
