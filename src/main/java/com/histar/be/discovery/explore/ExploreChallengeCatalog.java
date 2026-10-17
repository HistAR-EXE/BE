package com.histar.be.discovery.explore;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Versioned grading data is private to the server, never returned in a playable view. */
public final class ExploreChallengeCatalog {
    public static final UUID LOCATION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String CONCLUSION = "investigation:cu-chi-conclusion";
    public static final Set<String> CLUES = Set.of("clue:ev-01", "clue:ev-02", "clue:ev-03", "clue:ev-04");
    public record Challenge(int correctOption, int options, int xp) {}
    public static final Map<String, Challenge> QUIZZES = Map.ofEntries(
            Map.entry("depth_quiz:surface", new Challenge(1, 4, 15)),
            Map.entry("depth_quiz:layer1", new Challenge(1, 4, 15)),
            Map.entry("depth_quiz:layer2", new Challenge(0, 4, 15)),
            Map.entry("depth_quiz:layer3", new Challenge(2, 4, 15)),
            Map.entry("cipher:hoang-cam", new Challenge(1, 3, 50)),
            Map.entry("cipher:two-wells", new Challenge(0, 3, 75)),
            Map.entry("cipher:sandals", new Challenge(1, 3, 100)),
            Map.entry("dossier:architecture", new Challenge(1, 3, 100)),
            Map.entry("dossier:trap-warfare", new Challenge(0, 3, 120)),
            Map.entry("dossier:underground-life", new Challenge(1, 3, 90)),
            Map.entry("dossier:command-center", new Challenge(0, 3, 95)),
            Map.entry("dossier:tunnel-rats", new Challenge(0, 3, 110)),
            Map.entry("dossier:ben-duoc", new Challenge(0, 3, 100)));

    private ExploreChallengeCatalog() {}

    public static boolean grade(String key, Integer answer) {
        Challenge challenge = QUIZZES.get(key);
        if (challenge == null || answer == null || answer < 0 || answer >= challenge.options()) {
            throw new BusinessRuleException("Câu trả lời hoặc thử thách không hợp lệ.");
        }
        return answer == challenge.correctOption();
    }

    public static int xp(String key) {
        if (key == null) return 0;
        if (QUIZZES.containsKey(key)) return QUIZZES.get(key).xp();
        if (CLUES.contains(key)) return key.equals("clue:ev-04") ? 40 : 30;
        if (CONCLUSION.equals(key)) return 100;
        return 0;
    }
}
