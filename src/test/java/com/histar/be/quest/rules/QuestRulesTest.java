package com.histar.be.quest.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.quest.rules.QuestRules.Facts;
import com.histar.be.quest.rules.QuestRules.StepDef;
import com.histar.be.quest.rules.QuestRules.StepState;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestRulesTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-05T00:00:00Z");
    private static final Instant START = Instant.parse("2026-10-03T00:00:00Z");

    private static UUID id(int n) {
        return new UUID(0, n);
    }

    private static final List<StepDef> JOURNEY = List.of(
            new StepDef(id(1), 1, "scene:A", "tour", true, false),
            new StepDef(id(2), 2, "scene:B", "tour", true, false),
            new StepDef(id(3), 3, "xuanay:bep-hoang-cam", "xuanay", true, false),
            new StepDef(id(4), 4, "era:1948", "portal", false, true));

    @Test
    void ticksStepsByKeyNotByPosition() {
        Facts facts = new Facts(Map.of("scene:B", T0, "xuanay:bep-hoang-cam", T1), Map.of(), null, false);
        var ev = QuestRules.evaluate(JOURNEY, facts);
        assertEquals(2, ev.requiredDone());
        assertEquals(3, ev.requiredTotal());
        assertEquals(StepState.TODO, ev.steps().get(0).state());
        assertEquals(StepState.DONE, ev.steps().get(1).state());
        assertEquals(id(1), ev.nextStepId());
        assertFalse(ev.allRequiredDone());
    }

    @Test
    void freeUserCompletesWithoutPremiumStep() {
        Facts facts = new Facts(
                Map.of("scene:A", T0, "scene:B", T0, "xuanay:bep-hoang-cam", T1), Map.of(), null, false);
        var ev = QuestRules.evaluate(JOURNEY, facts);
        assertTrue(ev.allRequiredDone());
        assertEquals(StepState.PREMIUM_LOCKED, ev.steps().get(3).state());
        assertNull(ev.nextStepId());
    }

    @Test
    void premiumUserIsPointedToOptionalStep() {
        Facts facts = new Facts(
                Map.of("scene:A", T0, "scene:B", T0, "xuanay:bep-hoang-cam", T1), Map.of(), null, true);
        assertEquals(id(4), QuestRules.evaluate(JOURNEY, facts).nextStepId());
    }

    @Test
    void preDiscoveredWhenDoneBeforeStartOrNotStarted() {
        assertTrue(QuestRules.isPreDiscovered(T0, START));
        assertFalse(QuestRules.isPreDiscovered(T1, START));
        assertTrue(QuestRules.isPreDiscovered(T1, null));
        assertFalse(QuestRules.isPreDiscovered(null, START));
    }

    @Test
    void stationStepsUseQrCheckinsCaseInsensitive() {
        List<StepDef> onsite = List.of(
                new StepDef(id(10), 1, "station:ST01", "station", true, false),
                new StepDef(id(11), 2, "station:st02", "station", true, false));
        Facts facts = new Facts(Map.of(), Map.of("ST01", T0, "ST02", T1), T0, false);
        assertTrue(QuestRules.evaluate(onsite, facts).allRequiredDone());
    }

    @Test
    void legacyQuestKeepsCheckinRequirement() {
        List<StepDef> legacy = QuestRules.legacySteps("k1, k2", null);
        assertEquals(3, legacy.size());
        assertEquals("checkin", legacy.get(2).actionType());
        assertEquals(2, QuestRules.legacySteps("k1,k2", "discovery").size());
        assertTrue(QuestRules.evaluate(QuestRules.legacySteps(null, null), new Facts(Map.of(), Map.of(), T0, false))
                .allRequiredDone());
    }

    @Test
    void questWithoutRequiredStepsNeverCompletes() {
        assertFalse(QuestRules.evaluate(QuestRules.legacySteps(null, "discovery"), Facts.empty()).allRequiredDone());
    }

    @Test
    void helpers() {
        assertEquals(List.of("a", "b"), QuestRules.parseKeys(" a, ,b "));
        assertEquals("ONSITE", QuestRules.normalizeTrack("onsite"));
        assertEquals("ONLINE", QuestRules.normalizeTrack(null));
        assertTrue(QuestRules.isOnsiteAction("tour", "station:ST01"));
        assertFalse(QuestRules.isOnsiteAction("tour", "scene:x"));
    }
}
