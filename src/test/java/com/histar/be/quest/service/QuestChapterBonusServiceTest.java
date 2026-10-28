package com.histar.be.quest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.discovery.service.DiscoveryRewardService;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestChapterBonusServiceTest {
    private static final UUID SA_BAN_QUEST = UUID.fromString("33333333-3333-3333-3333-333333333334");
    @Mock UserDiscoveryRepository discoveries;
    @Mock QuestRepository quests;
    @Mock QuestUnlockGate gate;
    @Mock DiscoveryRewardService rewards;
    @Mock ProfilePointsService points;
    @InjectMocks QuestChapterBonusService service;

    @Test
    void correctQuizRecordsStepBeforeAwardingOnce() {
        UUID user = UUID.randomUUID();
        UUID location = UUID.randomUUID();
        Quest quest = Quest.builder().id(SA_BAN_QUEST).locationId(location).build();
        when(quests.findById(SA_BAN_QUEST)).thenReturn(Optional.of(quest));
        when(quests.findByLocationId(location)).thenReturn(java.util.List.of(quest));
        when(points.awardOnce(eq(user), any(String.class), eq(50))).thenReturn(50);

        var result = service.claim(user, location, QuestChapterBonusService.SA_BAN_STEP);

        assertEquals(50, result.awarded());
        verify(gate).assertUnlocked(user, quest);
        var order = inOrder(rewards, points);
        order.verify(rewards).record(user, location, QuestChapterBonusService.SA_BAN_STEP, "sa_ban_quiz");
        order.verify(points).awardOnce(eq(user), any(String.class), eq(50));
        verify(discoveries).saveAndFlush(any());
    }
}
