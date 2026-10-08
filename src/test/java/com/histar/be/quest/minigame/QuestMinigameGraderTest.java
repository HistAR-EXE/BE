package com.histar.be.quest.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
