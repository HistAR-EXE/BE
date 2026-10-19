package com.histar.be.quest.rules;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import java.util.List;

/** Chuẩn hoá bước của một nhiệm vụ: quest_steps là nguồn duy nhất; nhiệm vụ cũ chưa có bước thì dựng từ dữ liệu cũ. */
public final class QuestStepDefs {

    private QuestStepDefs() {}

    public static List<QuestRules.StepDef> forQuest(Quest quest, List<QuestStep> steps) {
        if (steps != null && !steps.isEmpty()) {
            return steps.stream().map(QuestStepDefs::from).toList();
        }
        return QuestRules.legacySteps(quest.getStepDiscoveryKeys(), quest.getCompletionTrigger());
    }

    public static QuestRules.StepDef from(QuestStep step) {
        boolean premium = Boolean.TRUE.equals(step.getPremium());
        // Bước Premium luôn là tùy chọn (dữ liệu sai cũng không được chặn người dùng free).
        boolean required = !premium && !Boolean.FALSE.equals(step.getRequired());
        return new QuestRules.StepDef(
                step.getId(),
                step.getStepOrder() == null ? 0 : step.getStepOrder(),
                step.getUnlockKey(),
                step.getActionType(),
                required,
                premium);
    }
}
