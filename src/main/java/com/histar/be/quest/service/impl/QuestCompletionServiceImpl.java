package com.histar.be.quest.service.impl;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.evaluator.QuestCompletionEvaluator;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.rules.QuestFactsLoader;
import com.histar.be.quest.rules.QuestRules;
import com.histar.be.profile.service.PremiumContentPolicy;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestProgressCompleter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hoàn thành mọi nhiệm vụ đang làm tại di tích đã đủ bước bắt buộc.
 * Không còn điều kiện "phải check-in trước": check-in là một bước của nhiệm vụ Tại di tích (v2).
 */
@Service
@RequiredArgsConstructor
public class QuestCompletionServiceImpl implements QuestCompletionService {

    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final QuestCompletionEvaluator questCompletionEvaluator;
    private final QuestProgressCompleter questProgressCompleter;
    private final QuestFactsLoader questFactsLoader;

    @Override
    @Transactional
    public List<QuestCompletedDto> tryComplete(UUID userId, UUID locationId, CompletionTrigger trigger) {
        List<QuestCompletedDto> completed = new ArrayList<>();
        if (userId == null || locationId == null) {
            return completed;
        }
        QuestRules.Facts facts = null;
        for (Quest quest : questRepository.findByLocationId(locationId)) {
            var progressOpt = userQuestProgressRepository.findByUserIdAndQuestId(userId, quest.getId());
            if (progressOpt.isEmpty() || !QuestStatus.IN_PROGRESS.equals(progressOpt.get().getStatus())) {
                continue;
            }
            if (facts == null) {
                facts = questFactsLoader.load(userId, locationId, false);
            }
            if (PremiumContentPolicy.premiumQuest(quest) && !facts.premium()) continue;
            if (!questCompletionEvaluator.isSatisfied(quest, facts)) {
                continue;
            }
            questProgressCompleter
                    .completeIfInProgress(userId, quest.getId(), trigger)
                    .ifPresent(completed::add);
        }
        return completed;
    }
}
