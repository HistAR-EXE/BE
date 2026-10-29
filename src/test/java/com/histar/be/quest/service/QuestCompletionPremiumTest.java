package com.histar.be.quest.service;

import static org.mockito.Mockito.*;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.evaluator.QuestCompletionEvaluator;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.rules.QuestFactsLoader;
import com.histar.be.quest.rules.QuestRules;
import com.histar.be.quest.service.impl.QuestCompletionServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestCompletionPremiumTest {
    @Mock QuestRepository quests;
    @Mock UserQuestProgressRepository progress;
    @Mock QuestCompletionEvaluator evaluator;
    @Mock QuestProgressCompleter completer;
    @Mock QuestFactsLoader factsLoader;
    @InjectMocks QuestCompletionServiceImpl service;

    @Test void premiumUserCanCompleteEligibleOnlineQuest() {
        var user = UUID.randomUUID();
        var location = UUID.randomUUID();
        var quest = Quest.builder().id(UUID.randomUUID()).locationId(location).track("ONLINE").build();
        var facts = new QuestRules.Facts(Map.of(), Map.of(), null, true);
        when(quests.findByLocationId(location)).thenReturn(List.of(quest));
        when(progress.findByUserIdAndQuestId(user, quest.getId()))
                .thenReturn(Optional.of(UserQuestProgress.builder().status(QuestStatus.IN_PROGRESS).build()));
        when(factsLoader.load(user, location, true)).thenReturn(facts);
        when(evaluator.isSatisfied(quest, facts)).thenReturn(true);
        when(completer.completeIfInProgress(user, quest.getId(), CompletionTrigger.DISCOVERY))
                .thenReturn(Optional.empty());

        service.tryComplete(user, location, CompletionTrigger.DISCOVERY);

        verify(completer).completeIfInProgress(user, quest.getId(), CompletionTrigger.DISCOVERY);
    }

    @Test void freeUserCannotCompletePremiumOnlineQuest() {
        var user = UUID.randomUUID();
        var location = UUID.randomUUID();
        var quest = Quest.builder().id(UUID.randomUUID()).locationId(location).track("ONLINE").build();
        when(quests.findByLocationId(location)).thenReturn(List.of(quest));
        when(progress.findByUserIdAndQuestId(user, quest.getId()))
                .thenReturn(Optional.of(UserQuestProgress.builder().status(QuestStatus.IN_PROGRESS).build()));
        when(factsLoader.load(user, location, true)).thenReturn(QuestRules.Facts.empty());

        service.tryComplete(user, location, CompletionTrigger.DISCOVERY);

        verifyNoInteractions(evaluator, completer);
    }
}
