package com.histar.be.quest.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.profile.service.PremiumContentPolicy;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.evaluator.QuestCompletionEvaluator;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.rules.QuestFactsLoader;
import com.histar.be.quest.rules.QuestRules;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestProgressCompleterMiniGameTest {
    @Mock ProfilePointsService points;
    @Mock QuestRepository quests;
    @Mock QuestFactsLoader facts;
    @Mock QuestCompletionEvaluator evaluator;
    @Mock UserQuestProgressRepository progress;
    @InjectMocks QuestProgressCompleter completer;

    @Test void cannotCompleteMinigameQuestWithoutFourRequiredSteps() {
        UUID user = UUID.randomUUID();
        UUID location = UUID.randomUUID();
        Quest quest = Quest.builder().id(PremiumContentPolicy.FREE_MINIGAME_QUEST)
                .locationId(location).track("ONLINE").build();
        when(quests.findById(quest.getId())).thenReturn(Optional.of(quest));
        when(facts.load(user, location, false)).thenReturn(QuestRules.Facts.empty());

        assertThrows(BusinessRuleException.class, () -> completer.completeIfInProgress(user, quest.getId()));
        verifyNoInteractions(progress);
    }
}
