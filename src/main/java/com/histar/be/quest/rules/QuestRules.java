package com.histar.be.quest.rules;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Luật nhiệm vụ v2 — thuần logic, không phụ thuộc Spring/JPA để dễ kiểm thử.
 *
 * <ul>
 *   <li>Bước "xong" khi người dùng có bản ghi tương ứng: khóa khám phá, QR tại trạm, hoặc check-in.</li>
 *   <li>Khóa có từ trước khi nhận nhiệm vụ vẫn được tính ({@link #isPreDiscovered}).</li>
 *   <li>Nhiệm vụ đủ điều kiện hoàn thành khi mọi bước bắt buộc đã xong; bước tùy chọn (kể cả Premium) không chặn.</li>
 *   <li>Thứ tự bước chỉ là gợi ý: bước kế tiếp = bước bắt buộc đầu tiên chưa xong.</li>
 * </ul>
 */
public final class QuestRules {

    public static final String TRACK_ONLINE = "ONLINE";
    public static final String TRACK_ONSITE = "ONSITE";
    public static final String ACTION_STATION = "station";
    public static final String ACTION_CHECKIN = "checkin";
    public static final String STATION_PREFIX = "station:";
    /** Khóa bước check-in tổng hợp cho nhiệm vụ cũ chưa có quest_steps. */
    public static final String LEGACY_CHECKIN_KEY = "checkin";

    private QuestRules() {}

    public enum StepState { DONE, TODO, PREMIUM_LOCKED }

    /** Định nghĩa bước đã chuẩn hoá (từ quest_steps hoặc dựng từ dữ liệu cũ). */
    public record StepDef(UUID id, int order, String key, String actionType, boolean required, boolean premium) {}

    /**
     * Dữ kiện của một người dùng tại một di tích.
     *
     * @param discoveries khóa khám phá → thời điểm ghi nhận sớm nhất
     * @param stationCheckins mã trạm (viết hoa) → thời điểm quét QR sớm nhất
     * @param firstCheckinAt lần check-in đầu tiên tại di tích (bất kỳ phương thức), null nếu chưa có
     * @param premium tài khoản có quyền Premium
     */
    public record Facts(
            Map<String, Instant> discoveries,
            Map<String, Instant> stationCheckins,
            Instant firstCheckinAt,
            boolean premium) {

        public static Facts empty() {
            return new Facts(Map.of(), Map.of(), null, false);
        }
    }

    public record StepResult(StepDef step, StepState state, Instant doneAt) {}

    public record Evaluation(
            List<StepResult> steps,
            int requiredDone,
            int requiredTotal,
            int optionalDone,
            int optionalTotal,
            UUID nextStepId) {

        /** Có ít nhất một bước bắt buộc và tất cả đã xong. Nhiệm vụ không có bước bắt buộc không tự hoàn thành. */
        public boolean allRequiredDone() {
            return requiredTotal > 0 && requiredDone >= requiredTotal;
        }
    }

    /** Thời điểm bước được hoàn thành, hoặc null nếu chưa xong. */
    public static Instant doneAt(StepDef step, Facts facts) {
        String action = normalize(step.actionType());
        String key = step.key() == null ? "" : step.key().trim();
        if (ACTION_STATION.equals(action) || key.startsWith(STATION_PREFIX)) {
            String code = key.startsWith(STATION_PREFIX) ? key.substring(STATION_PREFIX.length()) : key;
            return facts.stationCheckins().get(code.trim().toUpperCase(Locale.ROOT));
        }
        if (ACTION_CHECKIN.equals(action)) {
            return facts.firstCheckinAt();
        }
        return facts.discoveries().get(key);
    }

    public static Evaluation evaluate(List<StepDef> steps, Facts facts) {
        List<StepDef> ordered = new ArrayList<>(steps);
        ordered.sort(Comparator.comparingInt(StepDef::order));
        List<StepResult> results = new ArrayList<>(ordered.size());
        int requiredDone = 0;
        int requiredTotal = 0;
        int optionalDone = 0;
        int optionalTotal = 0;
        UUID nextRequired = null;
        UUID nextOptional = null;
        for (StepDef step : ordered) {
            Instant at = doneAt(step, facts);
            StepState state;
            if (at != null) {
                state = StepState.DONE;
            } else if (step.premium() && !facts.premium()) {
                state = StepState.PREMIUM_LOCKED;
            } else {
                state = StepState.TODO;
            }
            results.add(new StepResult(step, state, at));
            if (step.required()) {
                requiredTotal++;
                if (state == StepState.DONE) {
                    requiredDone++;
                } else if (nextRequired == null) {
                    nextRequired = step.id();
                }
            } else {
                optionalTotal++;
                if (state == StepState.DONE) {
                    optionalDone++;
                } else if (state == StepState.TODO && nextOptional == null) {
                    nextOptional = step.id();
                }
            }
        }
        UUID next = nextRequired != null ? nextRequired : nextOptional;
        return new Evaluation(List.copyOf(results), requiredDone, requiredTotal, optionalDone, optionalTotal, next);
    }

    /** Bước đã xong trước khi nhận nhiệm vụ (hoặc nhiệm vụ chưa được nhận). */
    public static boolean isPreDiscovered(Instant doneAt, Instant startedAt) {
        return doneAt != null && (startedAt == null || doneAt.isBefore(startedAt));
    }

    /**
     * Nhiệm vụ cũ chưa có quest_steps: dựng bước từ {@code step_discovery_keys}; nếu cách hoàn thành không phải
     * "discovery" thì thêm một bước check-in bắt buộc — giữ nguyên hành vi trước v2.
     */
    public static List<StepDef> legacySteps(String stepDiscoveryKeys, String completionTrigger) {
        List<StepDef> steps = new ArrayList<>();
        int order = 1;
        if (stepDiscoveryKeys != null && !stepDiscoveryKeys.isBlank()) {
            for (String raw : Arrays.asList(stepDiscoveryKeys.split(","))) {
                String key = raw.trim();
                if (key.isEmpty()) {
                    continue;
                }
                steps.add(new StepDef(legacyId(key), order++, key, "discovery", true, false));
            }
        }
        if (!"discovery".equalsIgnoreCase(completionTrigger == null ? "" : completionTrigger.trim())) {
            steps.add(new StepDef(legacyId(LEGACY_CHECKIN_KEY), order, LEGACY_CHECKIN_KEY, ACTION_CHECKIN, true, false));
        }
        return steps;
    }

    /** Khóa dùng để mở nhiệm vụ (unlock_discovery_keys), đã bỏ khoảng trắng và ô trống. */
    public static List<String> parseKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static boolean isOnsiteAction(String actionType, String key) {
        String action = normalize(actionType);
        return ACTION_STATION.equals(action)
                || ACTION_CHECKIN.equals(action)
                || (key != null && key.trim().startsWith(STATION_PREFIX));
    }

    public static String normalizeTrack(String track) {
        return TRACK_ONSITE.equalsIgnoreCase(track == null ? "" : track.trim()) ? TRACK_ONSITE : TRACK_ONLINE;
    }

    private static UUID legacyId(String key) {
        return UUID.nameUUIDFromBytes(("legacy-step:" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
