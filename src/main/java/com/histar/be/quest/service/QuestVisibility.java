package com.histar.be.quest.service;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuestVisibility {

    public boolean visible(Quest quest) {
        return true;
    }

    public boolean gameVisible(List<QuestStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return true;
        }
        return steps.stream().anyMatch(step -> visible(step.getQuest()));
    }
}
