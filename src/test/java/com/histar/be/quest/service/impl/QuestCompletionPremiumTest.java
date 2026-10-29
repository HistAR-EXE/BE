package com.histar.be.quest.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.evaluator.QuestCompletionEvaluator;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.rules.QuestFactsLoader;
import com.histar.be.quest.rules.QuestRules;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestProgressCompleter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestCompletionPremiumTest {
    @Test
    void completedPremiumStepsCanFinishAnOnlineQuest() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Quest quest = Quest.builder().id(questId).locationId(locationId).track("ONLINE").build();
        QuestRepository quests = mock(QuestRepository.class);
        UserQuestProgressRepository progress = mock(UserQuestProgressRepository.class);
        QuestCompletionEvaluator evaluator = mock(QuestCompletionEvaluator.class);
        QuestProgressCompleter completer = mock(QuestProgressCompleter.class);
        QuestFactsLoader factsLoader = mock(QuestFactsLoader.class);
        QuestRules.Facts premiumFacts = new QuestRules.Facts(Map.of(), Map.of(), null, true);
        when(quests.findByLocationId(locationId)).thenReturn(List.of(quest));
        when(progress.findByUserIdAndQuestId(userId, questId)).thenReturn(Optional.of(
                UserQuestProgress.builder().status(QuestStatus.IN_PROGRESS).build()));
        when(factsLoader.load(userId, locationId, true)).thenReturn(premiumFacts);
        when(evaluator.isSatisfied(quest, premiumFacts)).thenReturn(true);
        when(completer.completeIfInProgress(userId, questId, CompletionTrigger.DISCOVERY))
                .thenReturn(Optional.of(new QuestCompletedDto(questId, 100, List.of())));

        var service = new QuestCompletionServiceImpl(quests, progress, evaluator, completer, factsLoader);
        assertEquals(questId, service.tryComplete(userId, locationId, CompletionTrigger.DISCOVERY).get(0).questId());

        verify(completer).completeIfInProgress(userId, questId, CompletionTrigger.DISCOVERY);
    }
}
