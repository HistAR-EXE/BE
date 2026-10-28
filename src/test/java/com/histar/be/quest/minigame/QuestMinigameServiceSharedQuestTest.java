package com.histar.be.quest.minigame;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.observability.ProductionMetrics;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestUnlockGate;
import com.histar.be.quest.service.QuestVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class QuestMinigameServiceSharedQuestTest {
    @Test
    void replayAfterUpgradeRecordsPreviouslyLockedStepWithoutReawardingFreeStep() {
        UUID user = UUID.randomUUID();
        UUID location = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        Quest freeQuest = Quest.builder().locationId(location).build();
        Quest premiumQuest = Quest.builder().locationId(location).build();
        QuestStep free = QuestStep.builder().quest(freeQuest).unlockKey("minigame:free").build();
        QuestStep premium = QuestStep.builder().quest(premiumQuest).unlockKey("minigame:premium").build();
        QuestMinigame game = new QuestMinigame();
        game.setId(gameId);
        game.setEngine("im_lang");
        game.setConfig("{\"levels\":[{\"id\":\"lv\",\"holdMs\":3000}]}");

        QuestMinigameRepository games = mock(QuestMinigameRepository.class);
        UserGameResultRepository results = mock(UserGameResultRepository.class);
        QuestStepRepository steps = mock(QuestStepRepository.class);
        DiscoveryService discoveries = mock(DiscoveryService.class);
        ProfilePointsService points = mock(ProfilePointsService.class);
        QuestCompletionService completions = mock(QuestCompletionService.class);
        QuestVisibility visibility = mock(QuestVisibility.class);
        QuestUnlockGate gate = mock(QuestUnlockGate.class);
        QuestMinigameService service = new QuestMinigameService(games, results, steps, discoveries,
                points, completions, visibility, gate, mock(ArtifactService.class),
                new ObjectMapper(), mock(ProductionMetrics.class));

        when(games.findById(gameId)).thenReturn(Optional.of(game));
        when(steps.findByMinigameId(gameId)).thenReturn(List.of(free, premium));
        when(visibility.gameVisible(any())).thenReturn(true);
        when(visibility.visible(any())).thenReturn(true);
        UserGameResult saved = new UserGameResult();
        saved.setAttempts(2);
        when(results.findByUserIdAndGameId(user, gameId)).thenReturn(Optional.of(saved));
        when(discoveries.record(user, free.getUnlockKey(), location)).thenReturn(true, false);
        when(discoveries.record(user, premium.getUnlockKey(), location)).thenReturn(true);
        AtomicBoolean upgraded = new AtomicBoolean(false);
        doAnswer(invocation -> {
            if (invocation.getArgument(1) == premiumQuest && !upgraded.get()) {
                throw new BusinessRuleException("Premium required");
            }
            return null;
        }).when(gate).assertUnlocked(eq(user), any());
        MinigameAttempt attempt = new MinigameAttempt("lv", null, null, null, null,
                null, null, null, null, 3000, null);

        assertTrue(service.submit(user, gameId, attempt).discoveryRecorded());
        verify(discoveries, never()).record(user, premium.getUnlockKey(), location);

        upgraded.set(true);
        assertTrue(service.submit(user, gameId, attempt).discoveryRecorded());
        verify(discoveries).record(user, premium.getUnlockKey(), location);
    }
}
