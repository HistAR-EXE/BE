package com.histar.be.chat.knowledge;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Deterministic rendering of private progress facts; no model may rewrite these values. */
public final class UserProgressAnswerRenderer {

    private UserProgressAnswerRenderer() {}

    public static String render(String message, Map<String, Object> context, String stationCode) {
        String text = normalize(message);
        List<String> checkedIn = strings(context.get("checkedInStations"));
        List<String> remainingQuests = strings(context.get("remainingQuestNames"));
        List<String> unlockedArtifacts = strings(context.get("unlockedArtifactNames"));

        if (containsAny(text, "thuong", "diem tram", "xp")) {
            return "Trạng thái thưởng không được suy ra từ việc mở trang hoặc check-in. "
                    + "TimeLens chưa có dữ liệu live đủ để xác nhận phần thưởng của trạm này.";
        }
        if (containsAny(text, "co vat", "hien vat", "bo suu tap", "mo khoa")) {
            return unlockedArtifacts.isEmpty()
                    ? "Bạn chưa mở khóa cổ vật nào tại địa điểm này."
                    : "Bạn đã mở khóa " + unlockedArtifacts.size() + " cổ vật: "
                            + String.join(", ", unlockedArtifacts) + ".";
        }
        if (containsAny(text, "nhiem vu", "quest")) {
            return remainingQuests.isEmpty()
                    ? "Bạn không còn nhiệm vụ chưa hoàn thành tại địa điểm này."
                    : "Bạn còn " + remainingQuests.size() + " nhiệm vụ chưa hoàn thành: "
                            + String.join(", ", remainingQuests) + ".";
        }
        if (containsAny(text, "check-in", "check in", "checkin")) {
            String requestedStation = stationCode == null ? stationMention(text) : stationCode.toUpperCase(Locale.ROOT);
            if (requestedStation != null) {
                boolean present = checkedIn.stream().anyMatch(requestedStation::equalsIgnoreCase);
                return present
                        ? "Bạn đã check-in " + requestedStation + "."
                        : "Bạn chưa check-in " + requestedStation + ".";
            }
            return checkedIn.isEmpty()
                    ? "Bạn chưa check-in trạm nào tại địa điểm này."
                    : "Bạn đã check-in " + checkedIn.size() + " trạm: " + String.join(", ", checkedIn) + ".";
        }
        return "Bạn đã hoàn thành " + checkedIn.size() + " trạm tại địa điểm này."
                + (checkedIn.isEmpty() ? "" : " Các trạm: " + String.join(", ", checkedIn) + ".");
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private static String stationMention(String text) {
        var matcher = java.util.regex.Pattern.compile("(?:st|tram\\s*)(0?[1-9][0-9]?)").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return "ST%02d".formatted(Integer.parseInt(matcher.group(1)));
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return decomposed.replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
