package com.histar.be.chat.knowledge;

import jakarta.validation.constraints.Size;
import java.util.Locale;

/** Navigation context supplied by the client. It is never proof of physical presence or check-in. */
public record ChatPageContext(
        @Size(max = 64) String featureKey,
        @Size(max = 256) String route,
        @Size(max = 64) String siteCode,
        @Size(max = 32) String stationCode) {

    public boolean isStructurallyValid() {
        return within(featureKey, 64)
                && within(route, 256)
                && within(siteCode, 64)
                && within(stationCode, 32)
                && isInternalRoute(route);
    }

    public ChatPageContext normalized() {
        return new ChatPageContext(
                upper(featureKey),
                trim(route),
                lower(siteCode),
                upper(stationCode));
    }

    private static boolean within(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    private static boolean isInternalRoute(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String route = value.trim();
        return route.startsWith("/") && !route.startsWith("//") && !route.contains("\\");
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upper(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String lower(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
