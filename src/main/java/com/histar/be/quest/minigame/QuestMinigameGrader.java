package com.histar.be.quest.minigame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class QuestMinigameGrader {

    /** Số lần thả sai tối đa của Xếp tầng trước khi lượt chơi kết thúc (0 sao). */
    static final int XEP_TANG_MAX_MISTAKES = 8;
    /** Số lần chạm hụt vượt quá số điểm cần tìm của Săn cổ vật trước khi lượt chơi kết thúc. */
    static final int SAN_CO_VAT_EXTRA_MISSES = 3;

    private QuestMinigameGrader() {}

    /**
     * {@code resolved = false} nghĩa là lượt chơi chưa xong: không lưu kết quả, chỉ trả {@code hint}
     * để FE phản hồi ngay (nóng/lạnh, hướng đào, thẻ đặt sai…). Gợi ý không lộ đáp án.
     */
    public record Grade(int stars, boolean passed, boolean resolved, String learnFact, String learnUrl, Map<String, Object> hint) {}

    public static ObjectNode publicLevel(String engine, ObjectNode level) {
        ObjectNode view = level.deepCopy();
        view.remove("fact");
        switch (engine) {
            case "xep_tang" -> {
                if (view.has("items") && view.get("items").isArray()) {
                    view.get("items").forEach(item -> {
                        if (item instanceof ObjectNode object) {
                            object.remove("floorId");
                        }
                    });
                }
            }
            case "soi_nap" -> {
                view.remove("targetX");
                view.remove("targetY");
                view.remove("radius");
            }
            case "dao_gap" -> {
                view.remove("targetAngle");
                view.remove("tolerance");
            }
            case "san_co_vat" -> {
                view.remove("answerCount");
                view.remove("hotspots");
                view.remove("radius");
            }
            case "duong_khoi" -> {
                if (view.has("cells") && view.get("cells").isArray()) {
                    view.get("cells").forEach(cell -> {
                        if (cell instanceof ObjectNode object) {
                            object.remove("rotation");
                        }
                    });
                }
            }
            case "phong_van" -> {
                if (view.has("topics") && view.get("topics").isArray()) {
                    view.get("topics").forEach(topic -> {
                        if (topic instanceof ObjectNode object) {
                            object.remove("keywords");
                        }
                    });
                }
            }
            default -> view.remove("answer");
        }
        return view;
    }

    public static Grade grade(String engine, JsonNode config, JsonNode level, MinigameAttempt attempt) {
        String fact = text(config, "facts", 0, "text");
        String url = text(config, "facts", 0, "url");
        if (level.has("fact") && level.get("fact").isObject()) {
            fact = level.get("fact").path("text").asText(fact);
            url = level.get("fact").path("url").asText(url);
        }
        int stars = switch (engine) {
            case "xep_tang" -> gradeXepTang(level, attempt);
            case "soi_nap" -> gradeSoiNap(level, attempt);
            case "dao_gap" -> gradeDaoGap(level, attempt);
            case "san_co_vat" -> gradeSanCoVat(level, attempt);
            case "duong_khoi" -> gradeDuongKhoi(level, attempt);
            case "im_lang" -> gradeImLang(level, attempt);
            case "phong_van" -> gradePhongVan(level, attempt);
            default -> 0;
        };
        boolean resolved = switch (engine) {
            case "soi_nap" -> soiResolved(level, attempt);
            case "dao_gap" -> daoResolved(level, attempt, stars);
            case "xep_tang" -> xepResolved(level, attempt, stars);
            case "duong_khoi" -> duongResolved(attempt, stars);
            case "san_co_vat" -> sanResolved(level, attempt, stars);
            default -> true;
        };
        Map<String, Object> hint = resolved ? Map.of() : hint(engine, level, attempt);
        return new Grade(stars, stars >= 1, resolved, fact, url, hint);
    }

    static Map<String, Object> hint(String engine, JsonNode level, MinigameAttempt attempt) {
        Map<String, Object> hint = new LinkedHashMap<>();
        switch (engine) {
            case "soi_nap" -> {
                List<MinigameAttempt.Tap> taps = attempt.taps() == null ? List.of() : attempt.taps();
                hint.put("exposures", taps.size());
                if (!taps.isEmpty()) {
                    MinigameAttempt.Tap last = taps.get(taps.size() - 1);
                    double radius = level.path("radius").asDouble(8);
                    double d = distance(last.x(), last.y(), level.path("targetX").asDouble(), level.path("targetY").asDouble());
                    hint.put("warmth", d <= radius * 2 ? "HOT" : d <= radius * 4 ? "WARM" : "COLD");
                }
            }
            case "dao_gap" -> {
                List<Double> angles = attempt.angles() == null ? List.of() : attempt.angles();
                hint.put("tries", angles.size());
                if (!angles.isEmpty()) {
                    double last = angles.get(angles.size() - 1);
                    double target = level.path("targetAngle").asDouble();
                    double tolerance = level.path("tolerance").asDouble(8);
                    hint.put("direction", target > last ? "HIGHER" : "LOWER");
                    hint.put("near", Math.abs(target - last) <= tolerance * 3);
                }
            }
            case "xep_tang" -> {
                Map<String, String> answer = xepAnswer(level);
                Map<String, String> last = lastDrops(attempt);
                List<String> wrong = new ArrayList<>();
                for (Map.Entry<String, String> entry : last.entrySet()) {
                    String expected = answer.get(entry.getKey());
                    if (expected != null && !expected.equals(entry.getValue())) {
                        wrong.add(entry.getKey());
                    }
                }
                hint.put("wrong", wrong);
                hint.put("mistakes", xepMistakes(answer, attempt));
                hint.put("mistakesLeft", Math.max(0, XEP_TANG_MAX_MISTAKES - xepMistakes(answer, attempt)));
            }
            case "duong_khoi" -> hint.put("wrongCells", duongWrongCells(level, attempt));
            case "san_co_vat" -> {
                SanProgress progress = sanProgress(level, attempt);
                hint.put("found", progress.found());
                hint.put("total", progress.total());
                hint.put("misses", progress.misses());
                hint.put("lastHit", progress.lastHit());
                hint.put("missesLeft", Math.max(0, progress.total() + SAN_CO_VAT_EXTRA_MISSES - progress.misses()));
            }
            default -> {
                // Không có gợi ý.
            }
        }
        return hint;
    }

    private static boolean soiResolved(JsonNode level, MinigameAttempt attempt) {
        if (attempt.taps() == null || attempt.taps().isEmpty()) {
            return false;
        }
        if (gradeSoiNap(level, attempt) > 0) {
            return true;
        }
        int exposures = 0;
        double targetX = level.path("targetX").asDouble();
        double targetY = level.path("targetY").asDouble();
        double radius = level.path("radius").asDouble(8);
        for (MinigameAttempt.Tap tap : attempt.taps()) {
            if (tap.atMs() > 10_000) {
                return true;
            }
            if (distance(tap.x(), tap.y(), targetX, targetY) <= radius) {
                return true;
            }
            exposures++;
            if (exposures >= 3) {
                return true;
            }
        }
        return false;
    }

    private static boolean daoResolved(JsonNode level, MinigameAttempt attempt, int stars) {
        if (stars > 0) {
            return true;
        }
        return attempt.angles() != null && attempt.angles().size() >= 6;
    }

    private static boolean xepResolved(JsonNode level, MinigameAttempt attempt, int stars) {
        if (stars > 0 || attempt.drops() == null || attempt.drops().isEmpty()) {
            return true;
        }
        return xepMistakes(xepAnswer(level), attempt) >= XEP_TANG_MAX_MISTAKES;
    }

    private static boolean duongResolved(MinigameAttempt attempt, int stars) {
        return stars > 0 || attempt.drops() == null || attempt.drops().isEmpty();
    }

    private static boolean sanResolved(JsonNode level, MinigameAttempt attempt, int stars) {
        if (level.has("answerCount") || stars > 0 || attempt.taps() == null) {
            return true;
        }
        SanProgress progress = sanProgress(level, attempt);
        if (progress.total() == 0 || progress.found() >= progress.total()) {
            return true;
        }
        if (progress.misses() >= progress.total() + SAN_CO_VAT_EXTRA_MISSES) {
            return true;
        }
        int limit = level.path("timeLimitMs").asInt(20_000);
        return attempt.durationMs() != null && attempt.durationMs() > limit * 2;
    }

    private static Map<String, String> xepAnswer(JsonNode level) {
        Map<String, String> answer = new HashMap<>();
        level.path("items").forEach(item -> answer.put(item.path("id").asText(), item.path("floorId").asText()));
        return answer;
    }

    private static Map<String, String> lastDrops(MinigameAttempt attempt) {
        Map<String, String> last = new LinkedHashMap<>();
        if (attempt.drops() == null) {
            return last;
        }
        for (MinigameAttempt.Drop drop : attempt.drops()) {
            if (drop.itemId() != null) {
                last.put(drop.itemId(), drop.floorId());
            }
        }
        return last;
    }

    private static int xepMistakes(Map<String, String> answer, MinigameAttempt attempt) {
        if (attempt.drops() == null) {
            return 0;
        }
        int wrong = 0;
        for (MinigameAttempt.Drop drop : attempt.drops()) {
            String expected = answer.get(drop.itemId());
            if (expected == null || !expected.equals(drop.floorId())) {
                wrong++;
            }
        }
        return wrong;
    }

    static int gradeXepTang(JsonNode level, MinigameAttempt attempt) {
        Map<String, String> answer = xepAnswer(level);
        if (answer.isEmpty() || attempt.drops() == null) {
            return 0;
        }
        Map<String, String> last = lastDrops(attempt);
        int wrong = xepMistakes(answer, attempt);
        for (Map.Entry<String, String> entry : answer.entrySet()) {
            if (!entry.getValue().equals(last.get(entry.getKey()))) {
                return 0;
            }
        }
        if (wrong == 0) {
            return 3;
        }
        if (wrong <= 2) {
            return 2;
        }
        return 1;
    }

    static int gradeSoiNap(JsonNode level, MinigameAttempt attempt) {
        double targetX = level.path("targetX").asDouble();
        double targetY = level.path("targetY").asDouble();
        double radius = level.path("radius").asDouble(8);
        if (attempt.taps() == null) {
            return 0;
        }
        int exposures = 0;
        for (MinigameAttempt.Tap tap : attempt.taps()) {
            boolean hit = distance(tap.x(), tap.y(), targetX, targetY) <= radius;
            if (hit) {
                if (tap.atMs() > 10_000) {
                    return 0;
                }
                if (exposures == 0) {
                    return 3;
                }
                if (exposures == 1) {
                    return 2;
                }
                if (exposures == 2) {
                    return 1;
                }
                return 0;
            }
            exposures++;
            if (exposures >= 3) {
                return 0;
            }
        }
        return 0;
    }

    static int gradeDaoGap(JsonNode level, MinigameAttempt attempt) {
        double target = level.path("targetAngle").asDouble();
        double tolerance = level.path("tolerance").asDouble(8);
        if (attempt.angles() == null || attempt.angles().isEmpty()) {
            return 0;
        }
        for (int i = 0; i < attempt.angles().size(); i++) {
            double angle = attempt.angles().get(i);
            if (Math.abs(angle - target) <= tolerance) {
                if (i == 0) {
                    return 3;
                }
                if (i <= 2) {
                    return 2;
                }
                if (i <= 5) {
                    return 1;
                }
                return 0;
            }
        }
        return 0;
    }

    record SanProgress(int found, int total, int misses, boolean lastHit) {}

    private static SanProgress sanProgress(JsonNode level, MinigameAttempt attempt) {
        double radius = level.path("radius").asDouble(8);
        int hotspotCount = level.path("hotspots").size();
        Set<Integer> found = new HashSet<>();
        int misses = 0;
        boolean lastHit = false;
        if (attempt.taps() == null) {
            return new SanProgress(0, hotspotCount, 0, false);
        }
        for (MinigameAttempt.Tap tap : attempt.taps()) {
            int hitIndex = -1;
            for (int i = 0; i < hotspotCount; i++) {
                if (found.contains(i)) {
                    continue;
                }
                JsonNode spot = level.path("hotspots").get(i);
                if (distance(tap.x(), tap.y(), spot.path("x").asDouble(), spot.path("y").asDouble()) <= radius) {
                    hitIndex = i;
                    break;
                }
            }
            lastHit = hitIndex >= 0;
            if (lastHit) {
                found.add(hitIndex);
            } else {
                misses++;
            }
        }
        return new SanProgress(found.size(), hotspotCount, misses, lastHit);
    }

    static int gradeSanCoVat(JsonNode level, MinigameAttempt attempt) {
        if (level.has("answerCount")) {
            if (attempt.count() == null) {
                return 0;
            }
            int diff = Math.abs(attempt.count() - level.path("answerCount").asInt());
            if (diff == 0) {
                return 3;
            }
            if (diff == 1) {
                return 2;
            }
            if (diff == 2) {
                return 1;
            }
            return 0;
        }
        int limit = level.path("timeLimitMs").asInt(20_000);
        int hotspotCount = level.path("hotspots").size();
        if (hotspotCount == 0 || attempt.taps() == null) {
            return 0;
        }
        SanProgress progress = sanProgress(level, attempt);
        if (progress.found() < hotspotCount) {
            return 0;
        }
        int misses = progress.misses();
        int duration = attempt.durationMs() == null ? limit + 1 : attempt.durationMs();
        if (misses == 0 && duration <= limit) {
            return 3;
        }
        if (misses < hotspotCount) {
            return 2;
        }
        return 1;
    }

    /**
     * Mỗi ô có "rotation" là một đoạn rãnh cần xoay đúng. Ô thẳng xoay 0 và 2 (hoặc 1 và 3) giống nhau.
     * Ô không có "rotation" (bếp, hầm khói, miệng thoát, đá) không chấm.
     * Có "parMs" thì thời gian quyết định số sao; không có thì đúng là 3 sao.
     */
    static int gradeDuongKhoi(JsonNode level, MinigameAttempt attempt) {
        boolean hasAnswer = false;
        for (JsonNode cell : level.path("cells")) {
            if (cell.has("rotation")) {
                hasAnswer = true;
                break;
            }
        }
        if (!hasAnswer || attempt.drops() == null) {
            return 0;
        }
        if (duongWrongCells(level, attempt) > 0) {
            return 0;
        }
        int par = level.path("parMs").asInt(0);
        if (par <= 0 || attempt.durationMs() == null || attempt.durationMs() <= par) {
            return 3;
        }
        return attempt.durationMs() <= par * 2 ? 2 : 1;
    }

    private static int duongWrongCells(JsonNode level, MinigameAttempt attempt) {
        Map<String, String> last = lastDrops(attempt);
        int wrong = 0;
        for (JsonNode cell : level.path("cells")) {
            if (!cell.has("rotation")) {
                continue;
            }
            int expected = cell.path("rotation").asInt();
            Integer actual = parseRotation(last.get(cell.path("id").asText()));
            int period = "straight".equals(cell.path("kind").asText()) ? 2 : 4;
            if (actual == null || Math.floorMod(actual, period) != Math.floorMod(expected, period)) {
                wrong++;
            }
        }
        return wrong;
    }

    private static Integer parseRotation(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static int gradeImLang(JsonNode level, MinigameAttempt attempt) {
        int need = level.path("holdMs").asInt(3000);
        double margin = level.path("marginRatio").asDouble(1.8);
        Double baseline = attempt.baselineRms();
        Double peak = attempt.peakRms();
        boolean micQuiet = attempt.quietMs() != null
                && attempt.quietMs() >= need
                && baseline != null
                && peak != null
                && peak <= Math.max(0.02, baseline * margin);
        if (micQuiet) {
            return 3;
        }
        if (attempt.holdMs() != null && attempt.holdMs() >= need) {
            return 2;
        }
        if ((attempt.quietMs() != null && attempt.quietMs() >= need / 2)
                || (attempt.holdMs() != null && attempt.holdMs() >= need / 2)) {
            return 1;
        }
        return 0;
    }

    static int gradePhongVan(JsonNode level, MinigameAttempt attempt) {
        String said = fold(attempt.transcript());
        if (said.isBlank()) {
            return 0;
        }
        int hit = 0;
        for (JsonNode topic : level.path("topics")) {
            boolean matched = false;
            for (JsonNode keyword : topic.path("keywords")) {
                String folded = fold(keyword.asText());
                if (!folded.isBlank() && said.contains(folded)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                hit++;
            }
        }
        return Math.min(hit, 3);
    }

    private static String fold(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static String text(JsonNode config, String array, int index, String field) {
        JsonNode facts = config.path(array);
        if (!facts.isArray() || facts.size() <= index) {
            return "";
        }
        return facts.get(index).path(field).asText("");
    }
}
