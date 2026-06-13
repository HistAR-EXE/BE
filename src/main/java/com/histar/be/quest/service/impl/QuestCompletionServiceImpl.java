package com.histar.be.quest.service.impl;

import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.evaluator.CheckinEventContext;
import com.histar.be.quest.evaluator.QuestCompletionEvaluator;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestProgressCompleter;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestCompletionServiceImpl implements QuestCompletionService {

    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final CheckinRepository checkinRepository;
    private final QuestCompletionEvaluator questCompletionEvaluator;
    private final QuestProgressCompleter questProgressCompleter;

    @Override
    @Transactional
    public List<QuestCompletedDto> tryComplete(UUID userId, UUID locationId, CompletionTrigger trigger) {
        List<QuestCompletedDto> completed = new ArrayList<>();
        if (locationId == null) {
            return completed;
        }
        boolean requirePriorCheckin = trigger != CompletionTrigger.CHECKIN;
        if (requirePriorCheckin && !checkinRepository.existsByUserIdAndLocationId(userId, locationId)) {
            return completed;
        }

        CheckinEventContext event = new CheckinEventContext(userId, locationId);
        for (Quest quest : questRepository.findByLocationId(locationId)) {
            var progressOpt = userQuestProgressRepository.findByUserIdAndQuestId(userId, quest.getId());
            if (progressOpt.isEmpty() || !QuestStatus.IN_PROGRESS.equals(progressOpt.get().getStatus())) {
                continue;
            }
            if (!questCompletionEvaluator.isSatisfied(quest, event)) {
                continue;
            }
            questProgressCompleter
                    .completeIfInProgress(userId, quest.getId(), trigger)
                    .ifPresent(completed::add);
        }
        return completed;
    }
}
