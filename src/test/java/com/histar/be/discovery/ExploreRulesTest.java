package com.histar.be.discovery;

import static org.junit.jupiter.api.Assertions.*;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.explore.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExploreRulesTest {
    @Test void wrongAnswerDoesNotPass() {
        assertFalse(ExploreChallengeCatalog.grade("depth_quiz:surface", 0));
        assertTrue(ExploreChallengeCatalog.grade("depth_quiz:surface", 1));
        assertThrows(BusinessRuleException.class, () -> ExploreChallengeCatalog.grade("depth_quiz:surface", 4));
        assertThrows(BusinessRuleException.class, () -> ExploreChallengeCatalog.grade("made-up", 1));
    }
    @Test void globalPointsCountForBadges() {
        var progress = ExploreBadgeRules.evaluate(new ExploreBadgeRules.Context(500, Set.of(), false, false));
        assertTrue(progress.get(0).eligible());
        assertEquals(100, progress.get(0).percent());
        assertFalse(progress.get(5).eligible());
    }
    @Test void oneCompletedDepthFullyUnlocksFirstBadge() {
        var progress = ExploreBadgeRules.evaluate(new ExploreBadgeRules.Context(
                15, Set.of("depth_quiz:surface"), false, false));
        assertTrue(progress.get(0).eligible());
        assertEquals(100, progress.get(0).percent());
    }
    @Test void viewingDoesNotCompleteDepthQuizOrUnlockBadge() {
        var progress = ExploreBadgeRules.evaluate(new ExploreBadgeRules.Context(
                0, Set.of("depth:surface", "feature:ai_chat"), false, false));
        assertFalse(progress.get(0).eligible());
        assertFalse(progress.get(4).eligible());
    }

    @Test void archiveMaximumMatchesAllServerRewards() {
        int total = ExploreChallengeCatalog.QUIZZES.values().stream().mapToInt(ExploreChallengeCatalog.Challenge::xp).sum()
                + ExploreChallengeCatalog.CLUES.stream().mapToInt(ExploreChallengeCatalog::xp).sum()
                + ExploreChallengeCatalog.xp(ExploreChallengeCatalog.CONCLUSION);
        assertEquals(1340, total + 7 * 30);
        assertEquals(30, ExploreChallengeCatalog.xp("video:tunnel-digging"));
        assertEquals(0, ExploreChallengeCatalog.xp("video:cedar-falls"));
    }

    @Test void videosAreRequiredForUpdatedScholarAndUltimateBadge() {
        var keys = new java.util.HashSet<>(ExploreChallengeCatalog.QUIZZES.keySet());
        keys.add("scene:verified"); keys.add("feature:diorama");
        var missing = ExploreBadgeRules.evaluate(new ExploreBadgeRules.Context(1340, keys, true, true));
        assertFalse(missing.get(6).eligible()); assertFalse(missing.get(8).eligible());
    }
    @Test void allNineBadgesRequireTheirPersistedEvidence() {
        var keys = new java.util.HashSet<>(ExploreChallengeCatalog.QUIZZES.keySet());
        keys.addAll(ExploreChallengeCatalog.CLUES);
        keys.add(ExploreChallengeCatalog.CONCLUSION);
        keys.add("scene:verified"); keys.add("feature:diorama");
        for (String video : java.util.List.of("tunnel-digging","liberation-gate","communication-station","guerrilla-house","women-house","war-artifacts","ho-chi-minh-space")) keys.add("video:" + video);
        var complete = ExploreBadgeRules.evaluate(new ExploreBadgeRules.Context(1340, keys, true, true));
        assertEquals(9, complete.size());
        assertTrue(complete.stream().allMatch(p -> p.eligible() && p.percent() == 100));
        var missingExperiences = ExploreBadgeRules.evaluate(new ExploreBadgeRules.Context(1130, Set.of(), false, false));
        assertTrue(missingExperiences.get(0).eligible());
        assertTrue(missingExperiences.subList(1, 9).stream().noneMatch(ExploreBadgeRules.Progress::eligible));
        assertTrue(missingExperiences.subList(1, 9).stream().allMatch(p -> p.percent() < 100));
    }
}
