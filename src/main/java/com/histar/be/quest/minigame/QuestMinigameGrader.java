package com.histar.be.quest.minigame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class QuestMinigameGrader {

    private QuestMinigameGrader() {}

    public record Grade(int stars, boolean passed, boolean resolved, String learnFact, String learnUrl) {}

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
            default -> true;
        };
        return new Grade(stars, stars >= 1, resolved, fact, url);
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

    static int gradeXepTang(JsonNode level, MinigameAttempt attempt) {
        Map<String, String> answer = new HashMap<>();
        level.path("items").forEach(item -> answer.put(item.path("id").asText(), item.path("floorId").asText()));
        if (answer.isEmpty() || attempt.drops() == null) {
            return 0;
        }
        Map<String, String> last = new HashMap<>();
        int wrong = 0;
        for (MinigameAttempt.Drop drop : attempt.drops()) {
            String expected = answer.get(drop.itemId());
            if (expected == null || !expected.equals(drop.floorId())) {
                wrong++;
            }
            if (drop.itemId() != null) {
                last.put(drop.itemId(), drop.floorId());
            }
        }
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
        double radius = level.path("radius").asDouble(8);
        int limit = level.path("timeLimitMs").asInt(20_000);
        Set<Integer> found = new HashSet<>();
        int hotspotCount = level.path("hotspots").size();
        if (hotspotCount == 0 || attempt.taps() == null) {
            return 0;
        }
        int misses = 0;
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
            if (hitIndex >= 0) {
                found.add(hitIndex);
            } else {
                misses++;
            }
        }
        if (found.size() < hotspotCount) {
            return 0;
        }
        int duration = attempt.durationMs() == null ? limit + 1 : attempt.durationMs();
        if (misses == 0 && duration <= limit) {
            return 3;
        }
        if (misses < hotspotCount) {
            return 2;
        }
        return 1;
    }

    static int gradeDuongKhoi(JsonNode level, MinigameAttempt attempt) {
        Map<String, String> answer = new HashMap<>();
        level.path("cells").forEach(cell -> answer.put(cell.path("id").asText(), cell.path("rotation").asText()));
        if (answer.isEmpty() || attempt.drops() == null) {
            return 0;
        }
        Map<String, String> last = new HashMap<>();
        for (MinigameAttempt.Drop drop : attempt.drops()) {
            if (drop.itemId() != null) {
                last.put(drop.itemId(), drop.floorId());
            }
        }
        for (Map.Entry<String, String> entry : answer.entrySet()) {
            if (!entry.getValue().equals(last.get(entry.getKey()))) {
                return 0;
            }
        }
        return 3;
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
