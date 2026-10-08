package com.histar.be.quest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestRewardSummaryTest {

    @Test
    void exposes_only_rewards_that_are_awarded_by_the_current_server_rules() {
        Quest quest = Quest.builder()
                .pointsReward(200)
                .steps(List.of(
                        QuestStep.builder().unlockKey("tour:sa-ban").xpPartial(50).build(),
                        QuestStep.builder().unlockKey("tour:other").xpPartial(20).build()))
                .build();

        QuestRewardSummary summary = QuestRewardSummary.from(quest);

        assertThat(summary.rewards())
                .extracting(QuestRewardResponse::type, QuestRewardResponse::xp, QuestRewardResponse::triggerKey)
                .containsExactly(
                        tuple(QuestRewardType.EXPLORE_XP, 10, null),
                        tuple(QuestRewardType.CHAPTER_REWARD, 50, "tour:sa-ban"),
                        tuple(QuestRewardType.QUEST_COMPLETION_REWARD, 200, null));
    }

    @Test
    void keeps_the_completion_reward_when_the_quest_has_no_bonus_step() {
        Quest quest = Quest.builder().pointsReward(0).steps(List.of()).build();

        QuestRewardSummary summary = QuestRewardSummary.from(quest);

        assertThat(summary.rewards())
                .extracting(QuestRewardResponse::type, QuestRewardResponse::xp)
                .containsExactly(
                        tuple(QuestRewardType.EXPLORE_XP, 10),
                        tuple(QuestRewardType.QUEST_COMPLETION_REWARD, 0));
    }
}
