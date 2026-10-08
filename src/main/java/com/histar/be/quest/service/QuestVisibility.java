package com.histar.be.quest.service;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QuestVisibility {

    public static final UUID DEV_MINIGAME_QUEST_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333336");

    private final boolean exposeDevMinigameQuest;

    public QuestVisibility(
            @Value("${histar.quests.expose-dev-minigame:false}") boolean exposeDevMinigameQuest) {
        this.exposeDevMinigameQuest = exposeDevMinigameQuest;
    }

    public boolean visible(Quest quest) {
        return exposeDevMinigameQuest || quest == null || !DEV_MINIGAME_QUEST_ID.equals(quest.getId());
    }

    public boolean gameVisible(List<QuestStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return true;
        }
        return steps.stream().anyMatch(step -> visible(step.getQuest()));
    }
}
