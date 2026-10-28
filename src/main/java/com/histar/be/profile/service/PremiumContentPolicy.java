package com.histar.be.profile.service;

import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.rules.QuestRules;
import java.util.Set;
import java.util.UUID;

/** Stable content keys used at the API boundary. Journey Pass is deliberately excluded. */
public final class PremiumContentPolicy {
    public static final UUID FREE_SAND_TABLE_QUEST = UUID.fromString("33333333-3333-3333-3333-333333333334");
    public static final UUID FREE_MINIGAME_QUEST = UUID.fromString("33333333-3333-3333-3333-333333333336");
    private static final Set<String> PREMIUM_DISCOVERIES = Set.of(
            "era:1948", "era:1968", "era:1975",
            "cipher:two-wells", "cipher:sandals",
            "clue:ev-03", "clue:ev-04", "investigation:cu-chi-conclusion",
            "dossier:architecture", "dossier:ben-duoc", "dossier:tunnel-rats", "dossier:trap-warfare");

    private PremiumContentPolicy() {}

    public static boolean premiumDiscovery(String key) {
        return PREMIUM_DISCOVERIES.contains(key);
    }

    public static boolean premiumQuest(Quest quest) {
        return QuestRules.TRACK_ONLINE.equals(QuestRules.normalizeTrack(quest.getTrack()))
                && !FREE_SAND_TABLE_QUEST.equals(quest.getId())
                && !FREE_MINIGAME_QUEST.equals(quest.getId());
    }
}
