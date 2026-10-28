package com.histar.be.profile.service;

import static org.junit.jupiter.api.Assertions.*;

import com.histar.be.quest.entity.Quest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PremiumContentPolicyTest {
    @Test void discoveryKeysMatchFreeMatrix() {
        assertFalse(PremiumContentPolicy.premiumDiscovery("cipher:hoang-cam"));
        assertFalse(PremiumContentPolicy.premiumDiscovery("clue:ev-02"));
        assertFalse(PremiumContentPolicy.premiumDiscovery("dossier:command-center"));
        for (String key : new String[] {"cipher:sandals", "clue:ev-03", "investigation:cu-chi-conclusion",
                "dossier:trap-warfare", "era:1948", "era:1968", "era:1975"}) {
            assertTrue(PremiumContentPolicy.premiumDiscovery(key), key);
        }
    }

    @Test void onlyTwoOnlineQuestsAreFree() {
        for (UUID id : new UUID[] {PremiumContentPolicy.FREE_SAND_TABLE_QUEST, PremiumContentPolicy.FREE_MINIGAME_QUEST}) {
            assertFalse(PremiumContentPolicy.premiumQuest(Quest.builder().id(id).track("ONLINE").build()));
        }
        assertTrue(PremiumContentPolicy.premiumQuest(Quest.builder().id(UUID.randomUUID()).track("ONLINE").build()));
        assertFalse(PremiumContentPolicy.premiumQuest(Quest.builder().id(UUID.randomUUID()).track("ONSITE").build()));
    }
}
