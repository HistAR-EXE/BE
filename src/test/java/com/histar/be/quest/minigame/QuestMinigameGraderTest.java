package com.histar.be.quest.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestMinigameGraderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void xepTangHidesAnswersAndScoresWrongDrops() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("""
                {"id":"fn-1","items":[{"id":"a","label":"xe tăng","floorId":"t1"}],"floors":[{"id":"t1","label":"Tầng 1"}]}
                """);
        ObjectNode pub = QuestMinigameGrader.publicLevel("xep_tang", level);
        assertFalse(pub.get("items").get(0).has("floorId"));

        assertEquals(3, QuestMinigameGrader.gradeXepTang(level, new MinigameAttempt("fn-1",
                List.of(new MinigameAttempt.Drop("a", "t1")), null, null, null)));
        assertEquals(2, QuestMinigameGrader.gradeXepTang(level, new MinigameAttempt("fn-1",
                List.of(new MinigameAttempt.Drop("a", "t2"), new MinigameAttempt.Drop("a", "t1")), null, null, null)));
        assertEquals(1, QuestMinigameGrader.gradeXepTang(level, new MinigameAttempt("fn-1",
                List.of(
                        new MinigameAttempt.Drop("a", "t2"),
                        new MinigameAttempt.Drop("a", "t3"),
                        new MinigameAttempt.Drop("a", "t2"),
                        new MinigameAttempt.Drop("a", "t1")),
                null, null, null)));
        assertEquals(0, QuestMinigameGrader.gradeXepTang(level, new MinigameAttempt("fn-1",
                List.of(new MinigameAttempt.Drop("a", "t2")), null, null, null)));
    }

    @Test
    void soiNapScoresExposuresAndTimeout() {
        ObjectNode level = mapper.createObjectNode();
        level.put("targetX", 30);
        level.put("targetY", 40);
        level.put("radius", 8);
        assertEquals(3, QuestMinigameGrader.gradeSoiNap(level, taps(tap(30, 40, 1000))));
        assertEquals(2, QuestMinigameGrader.gradeSoiNap(level, taps(tap(0, 0, 100), tap(30, 40, 2000))));
        assertEquals(1, QuestMinigameGrader.gradeSoiNap(level, taps(
                tap(0, 0, 100), tap(1, 1, 200), tap(30, 40, 3000))));
        assertEquals(0, QuestMinigameGrader.gradeSoiNap(level, taps(
                tap(0, 0, 100), tap(1, 1, 200), tap(2, 2, 300))));
        assertEquals(0, QuestMinigameGrader.gradeSoiNap(level, taps(tap(30, 40, 10_001))));
    }

    @Test
    void daoGapScoresAttemptIndex() {
        ObjectNode level = mapper.createObjectNode();
        level.put("targetAngle", 10);
        level.put("tolerance", 8);
        assertEquals(3, QuestMinigameGrader.gradeDaoGap(level, angles(10)));
        assertEquals(2, QuestMinigameGrader.gradeDaoGap(level, angles(0, 12)));
        assertEquals(1, QuestMinigameGrader.gradeDaoGap(level, angles(0, 40, -20, 10)));
        assertEquals(0, QuestMinigameGrader.gradeDaoGap(level, angles(0, 40, -20, 50, 60, -40, 10)));
        assertEquals(0, QuestMinigameGrader.gradeDaoGap(level, angles(80)));
    }

    @Test
    void sanCoVatScoresMissesAndTime() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("""
                {"timeLimitMs":10000,"radius":8,"hotspots":[{"x":10,"y":10},{"x":80,"y":80}]}
                """);
        assertEquals(3, QuestMinigameGrader.gradeSanCoVat(level, taps(2000, tap(10, 10, 0), tap(80, 80, 0))));
        assertEquals(2, QuestMinigameGrader.gradeSanCoVat(level, taps(2000, tap(0, 0, 0), tap(10, 10, 0), tap(80, 80, 0))));
        assertEquals(2, QuestMinigameGrader.gradeSanCoVat(level, taps(20_000, tap(10, 10, 0), tap(80, 80, 0))));
        assertEquals(1, QuestMinigameGrader.gradeSanCoVat(level, taps(2000,
                tap(0, 0, 0), tap(1, 1, 0), tap(10, 10, 0), tap(80, 80, 0))));
        assertEquals(0, QuestMinigameGrader.gradeSanCoVat(level, taps(2000, tap(10, 10, 0))));
        ObjectNode pub = QuestMinigameGrader.publicLevel("san_co_vat", level);
        assertFalse(pub.has("hotspots"));
        assertFalse(pub.has("answerCount"));
    }

    @Test
    void demDauScoresDistanceFromHiddenCount() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("{\"answerCount\":4}");
        assertEquals(3, QuestMinigameGrader.gradeSanCoVat(level, count(4)));
        assertEquals(2, QuestMinigameGrader.gradeSanCoVat(level, count(3)));
        assertEquals(1, QuestMinigameGrader.gradeSanCoVat(level, count(6)));
        assertEquals(0, QuestMinigameGrader.gradeSanCoVat(level, count(9)));
    }

    @Test
    void duongKhoiNeedsEveryRotation() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("""
                {"cells":[{"id":"a","rotation":1},{"id":"b","rotation":2}]}
                """);
        ObjectNode pub = QuestMinigameGrader.publicLevel("duong_khoi", level);
        assertFalse(pub.get("cells").get(0).has("rotation"));
        assertEquals(3, QuestMinigameGrader.gradeDuongKhoi(level, new MinigameAttempt("dk",
                List.of(new MinigameAttempt.Drop("a", "1"), new MinigameAttempt.Drop("b", "2")),
                null, null, null)));
        assertEquals(0, QuestMinigameGrader.gradeDuongKhoi(level, new MinigameAttempt("dk",
                List.of(new MinigameAttempt.Drop("a", "0"), new MinigameAttempt.Drop("b", "2")),
                null, null, null)));
    }

    @Test
    void imLangUsesAmbientMicOrFingerHold() {
        ObjectNode level = mapper.createObjectNode();
        level.put("holdMs", 3000);
        level.put("marginRatio", 1.8);
        assertEquals(3, QuestMinigameGrader.gradeImLang(level, signal(3000, 0.1, 0.15, 0)));
        assertEquals(2, QuestMinigameGrader.gradeImLang(level, signal(0, 0.1, 0.9, 3000)));
        assertEquals(1, QuestMinigameGrader.gradeImLang(level, signal(0, 0.1, 0.9, 1500)));
        assertEquals(0, QuestMinigameGrader.gradeImLang(level, signal(1000, 0.1, 0.9, 400)));
    }

    @Test
    void phongVanFoldsDiacritics() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("""
                {"topics":[
                  {"label":"Bếp","keywords":["khói"]},
                  {"label":"Nắp","keywords":["ngụy trang"]},
                  {"label":"Tầng","keywords":["địa đạo"]}
                ]}
                """);
        ObjectNode pub = QuestMinigameGrader.publicLevel("phong_van", level);
        assertFalse(pub.get("topics").get(0).has("keywords"));
        assertEquals(3, QuestMinigameGrader.gradePhongVan(level, transcript("khoi bay, nguy trang, dia dao")));
        assertEquals(2, QuestMinigameGrader.gradePhongVan(level, transcript("KHOI va nguy trang")));
        assertEquals(0, QuestMinigameGrader.gradePhongVan(level, transcript("")));
    }

    @Test
    void soiNapPendingHintsWarmthWithoutTarget() {
        ObjectNode level = mapper.createObjectNode();
        level.put("targetX", 50);
        level.put("targetY", 50);
        level.put("radius", 10);
        QuestMinigameGrader.Grade far = QuestMinigameGrader.grade("soi_nap", mapper.createObjectNode(), level, taps(tap(0, 0, 500)));
        assertFalse(far.resolved());
        assertEquals("COLD", far.hint().get("warmth"));
        assertEquals(1, far.hint().get("exposures"));
        assertFalse(far.hint().containsKey("targetX"));
        QuestMinigameGrader.Grade near = QuestMinigameGrader.grade("soi_nap", mapper.createObjectNode(), level,
                taps(tap(0, 0, 500), tap(65, 50, 900)));
        assertFalse(near.resolved());
        assertEquals("HOT", near.hint().get("warmth"));
        QuestMinigameGrader.Grade hit = QuestMinigameGrader.grade("soi_nap", mapper.createObjectNode(), level,
                taps(tap(0, 0, 500), tap(52, 50, 900)));
        assertTrue(hit.resolved());
        assertEquals(2, hit.stars());
        assertTrue(hit.hint().isEmpty());
    }

    @Test
    void daoGapPendingHintsDirection() {
        ObjectNode level = mapper.createObjectNode();
        level.put("targetAngle", 20);
        level.put("tolerance", 8);
        QuestMinigameGrader.Grade low = QuestMinigameGrader.grade("dao_gap", mapper.createObjectNode(), level, angles(-40));
        assertFalse(low.resolved());
        assertEquals("HIGHER", low.hint().get("direction"));
        assertEquals(false, low.hint().get("near"));
        QuestMinigameGrader.Grade high = QuestMinigameGrader.grade("dao_gap", mapper.createObjectNode(), level, angles(-40, 35));
        assertEquals("LOWER", high.hint().get("direction"));
        assertEquals(true, high.hint().get("near"));
        assertEquals(2, high.hint().get("tries"));
    }

    @Test
    void xepTangKeepsPlayingUntilEveryCardIsRight() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("""
                {"id":"fn","items":[{"id":"a","floorId":"t1"},{"id":"b","floorId":"t2"}]}
                """);
        QuestMinigameGrader.Grade wrong = QuestMinigameGrader.grade("xep_tang", mapper.createObjectNode(), level,
                new MinigameAttempt("fn", List.of(new MinigameAttempt.Drop("a", "t2"), new MinigameAttempt.Drop("b", "t2")), null, null, null));
        assertFalse(wrong.resolved());
        assertEquals(List.of("a"), wrong.hint().get("wrong"));
        assertEquals(1, wrong.hint().get("mistakes"));
        QuestMinigameGrader.Grade fixed = QuestMinigameGrader.grade("xep_tang", mapper.createObjectNode(), level,
                new MinigameAttempt("fn", List.of(
                        new MinigameAttempt.Drop("a", "t2"),
                        new MinigameAttempt.Drop("b", "t2"),
                        new MinigameAttempt.Drop("a", "t1")), null, null, null));
        assertTrue(fixed.resolved());
        assertEquals(2, fixed.stars());
        java.util.List<MinigameAttempt.Drop> many = new java.util.ArrayList<>();
        for (int i = 0; i < QuestMinigameGrader.XEP_TANG_MAX_MISTAKES; i++) {
            many.add(new MinigameAttempt.Drop("a", "t3"));
        }
        many.add(new MinigameAttempt.Drop("b", "t2"));
        QuestMinigameGrader.Grade givenUp = QuestMinigameGrader.grade("xep_tang", mapper.createObjectNode(), level,
                new MinigameAttempt("fn", many, null, null, null));
        assertTrue(givenUp.resolved());
        assertEquals(0, givenUp.stars());
    }

    @Test
    void duongKhoiTreatsStraightHalfTurnsAlikeAndScoresTime() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("""
                {"parMs":10000,"cells":[
                  {"id":"a","kind":"straight","rotation":1},
                  {"id":"b","kind":"elbow","rotation":2},
                  {"id":"s","kind":"stove"}]}
                """);
        List<MinigameAttempt.Drop> solved = List.of(new MinigameAttempt.Drop("a", "3"), new MinigameAttempt.Drop("b", "2"));
        assertEquals(3, QuestMinigameGrader.gradeDuongKhoi(level, new MinigameAttempt("dk", solved, null, null, 5000)));
        assertEquals(2, QuestMinigameGrader.gradeDuongKhoi(level, new MinigameAttempt("dk", solved, null, null, 15000)));
        assertEquals(1, QuestMinigameGrader.gradeDuongKhoi(level, new MinigameAttempt("dk", solved, null, null, 25000)));
        QuestMinigameGrader.Grade wrong = QuestMinigameGrader.grade("duong_khoi", mapper.createObjectNode(), level,
                new MinigameAttempt("dk", List.of(new MinigameAttempt.Drop("a", "0"), new MinigameAttempt.Drop("b", "0")), null, null, 4000));
        assertFalse(wrong.resolved());
        assertEquals(2, wrong.hint().get("wrongCells"));
    }

    @Test
    void sanCoVatHotspotsReportProgressUntilAllFound() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("""
                {"timeLimitMs":10000,"radius":8,"hotspots":[{"x":10,"y":10},{"x":80,"y":80}]}
                """);
        QuestMinigameGrader.Grade one = QuestMinigameGrader.grade("san_co_vat", mapper.createObjectNode(), level, taps(1000, tap(10, 10, 0)));
        assertFalse(one.resolved());
        assertEquals(1, one.hint().get("found"));
        assertEquals(true, one.hint().get("lastHit"));
        QuestMinigameGrader.Grade miss = QuestMinigameGrader.grade("san_co_vat", mapper.createObjectNode(), level,
                taps(1500, tap(10, 10, 0), tap(50, 50, 0)));
        assertEquals(false, miss.hint().get("lastHit"));
        QuestMinigameGrader.Grade done = QuestMinigameGrader.grade("san_co_vat", mapper.createObjectNode(), level,
                taps(2000, tap(10, 10, 0), tap(50, 50, 0), tap(80, 80, 0)));
        assertTrue(done.resolved());
        assertEquals(2, done.stars());
    }

    private static MinigameAttempt taps(MinigameAttempt.Tap... taps) {
        return new MinigameAttempt("lv", null, List.of(taps), null, null);
    }

    private static MinigameAttempt taps(int durationMs, MinigameAttempt.Tap... taps) {
        return new MinigameAttempt("lv", null, List.of(taps), null, durationMs);
    }

    private static MinigameAttempt.Tap tap(double x, double y, int atMs) {
        return new MinigameAttempt.Tap(x, y, atMs);
    }

    private static MinigameAttempt angles(double... values) {
        return new MinigameAttempt("lv", null, null, java.util.Arrays.stream(values).boxed().toList(), null);
    }

    private static MinigameAttempt count(int count) {
        return new MinigameAttempt("lv", null, null, null, null, count, null, null, null, null, null);
    }

    private static MinigameAttempt signal(int quietMs, double baseline, double peak, int holdMs) {
        return new MinigameAttempt("lv", null, null, null, null, null, quietMs, baseline, peak, holdMs, null);
    }

    private static MinigameAttempt transcript(String text) {
        return new MinigameAttempt("lv", null, null, null, null, null, null, null, null, null, text);
    }
}
