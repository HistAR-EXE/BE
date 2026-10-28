package com.histar.be.quest.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.profile.service.PremiumContentPolicy;
import com.histar.be.profile.service.TierAccessService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestUnlockGatePremiumTest {
    @Mock UserDiscoveryRepository discoveries;
    @Mock UserQuestProgressRepository progress;
    @Mock QuestRepository quests;
    @Mock TierAccessService tier;
    @InjectMocks QuestUnlockGate gate;
    UUID user;

    @BeforeEach void setup() { user = UUID.randomUUID(); }

    @Test void freeCanStartOnlyTwoOnlineQuestsAndOnsite() {
        assertDoesNotThrow(() -> gate.assertUnlocked(user, quest(PremiumContentPolicy.FREE_SAND_TABLE_QUEST, "ONLINE")));
        assertDoesNotThrow(() -> gate.assertUnlocked(user, quest(PremiumContentPolicy.FREE_MINIGAME_QUEST, "ONLINE")));
        assertDoesNotThrow(() -> gate.assertUnlocked(user, quest(UUID.randomUUID(), "ONSITE")));
        assertThrows(BusinessRuleException.class, () -> gate.assertUnlocked(user, quest(UUID.randomUUID(), "ONLINE")));
    }

    @Test void premiumCanStartOtherOnlineQuest() {
        when(tier.hasPremiumAccess(user)).thenReturn(true);
        assertDoesNotThrow(() -> gate.assertUnlocked(user, quest(UUID.randomUUID(), "ONLINE")));
    }

    private Quest quest(UUID id, String track) {
        return Quest.builder().id(id).track(track).build();
    }
}
