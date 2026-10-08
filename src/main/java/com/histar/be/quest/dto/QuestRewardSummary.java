package com.histar.be.quest.dto;

import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.service.QuestChapterBonusService;
import java.util.ArrayList;
import java.util.List;

public record QuestRewardSummary(List<QuestRewardResponse> rewards) {

    public static QuestRewardSummary from(Quest quest) {
        List<QuestRewardResponse> rewards = new ArrayList<>();
        rewards.add(new QuestRewardResponse(QuestRewardType.EXPLORE_XP, ProfilePointsService.XP_DISCOVERY, null));

        if (quest.getSteps() != null) {
            quest.getSteps().stream()
                    .filter(step -> QuestChapterBonusService.SA_BAN_STEP.equals(step.getUnlockKey()))
                    .findFirst()
                    .ifPresent(step -> rewards.add(new QuestRewardResponse(
                            QuestRewardType.CHAPTER_REWARD,
                            positiveOrDefault(step),
                            QuestChapterBonusService.SA_BAN_STEP)));
        }

        rewards.add(new QuestRewardResponse(
                QuestRewardType.QUEST_COMPLETION_REWARD,
                quest.getPointsReward() == null ? 0 : Math.max(0, quest.getPointsReward()),
                null));
        return new QuestRewardSummary(List.copyOf(rewards));
    }

    private static int positiveOrDefault(QuestStep step) {
        return step.getXpPartial() != null && step.getXpPartial() > 0 ? step.getXpPartial() : 50;
    }
}
