package com.histar.be.quest.evaluator;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.rules.QuestFactsLoader;
import com.histar.be.quest.rules.QuestRules;
import com.histar.be.quest.rules.QuestStepDefs;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Nhiệm vụ đủ điều kiện hoàn thành khi mọi bước bắt buộc (quest_steps) đã xong.
 * Nhiệm vụ cũ chưa có quest_steps dùng bước dựng từ step_discovery_keys (+ check-in nếu không phải "discovery").
 */
@Component
@RequiredArgsConstructor
public class QuestCompletionEvaluator {

    private final QuestStepRepository questStepRepository;
    private final QuestFactsLoader questFactsLoader;

    public boolean isSatisfied(Quest quest, CheckinEventContext context) {
        if (quest == null || context == null) {
            return false;
        }
        UUID locationId = quest.getLocationId();
        if (locationId == null || !locationId.equals(context.locationId())) {
            return false;
        }
        return isSatisfied(quest, questFactsLoader.load(context.userId(), locationId, false));
    }

    /** Dùng khi đã nạp sẵn dữ kiện của người dùng tại di tích của nhiệm vụ. */
    public boolean isSatisfied(Quest quest, QuestRules.Facts facts) {
        if (quest == null || facts == null) {
            return false;
        }
        var steps = QuestStepDefs.forQuest(quest, questStepRepository.findByQuestId(quest.getId()));
        return QuestRules.evaluate(steps, facts).allRequiredDone();
    }
}
