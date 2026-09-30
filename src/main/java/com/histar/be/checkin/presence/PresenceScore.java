package com.histar.be.checkin.presence;

/** Presence confidence scoring: QR +60, GPS +30, sequence +10 (max 100). */
public final class PresenceScore {

    public static final int QR_POINTS = 60;
    public static final int GPS_POINTS = 30;
    public static final int SEQUENCE_POINTS = 10;
    public static final int MAX_SCORE = QR_POINTS + GPS_POINTS + SEQUENCE_POINTS;

    private PresenceScore() {}

    public static int compute(boolean qrVerified, boolean gpsWithinRadius, boolean sequenceOk) {
        int score = 0;
        if (qrVerified) {
            score += QR_POINTS;
        }
        if (gpsWithinRadius) {
            score += GPS_POINTS;
        }
        if (sequenceOk) {
            score += SEQUENCE_POINTS;
        }
        return score;
    }

    /** Sequence bonus applies when the current station directly follows the previous completed one. */
    public static boolean isSequential(Integer previousSortOrder, Integer currentSortOrder) {
        return previousSortOrder != null
                && currentSortOrder != null
                && currentSortOrder == previousSortOrder + 1;
    }

    /**
     * Derives a station sort order from its code (trailing digits, e.g. {@code ST-03} -> 3).
     * Fallback when the stations module has no row for the code.
     */
    public static Integer sortOrderOf(String stationCode) {
        if (stationCode == null) {
            return null;
        }
        int end = stationCode.length();
        int start = end;
        while (start > 0 && Character.isDigit(stationCode.charAt(start - 1))) {
            start--;
        }
        if (start == end || end - start > 9) {
            return null;
        }
        return Integer.parseInt(stationCode.substring(start, end));
    }

    /** Prefers the stations-table {@code sort_order} when known; falls back to the code's trailing digits. */
    public static Integer resolveSortOrder(Integer stationTableSortOrder, String stationCode) {
        return stationTableSortOrder != null ? stationTableSortOrder : sortOrderOf(stationCode);
    }

    public static PresenceMethod methodFor(boolean qrVerified, boolean gpsWithinRadius) {
        if (qrVerified) {
            return PresenceMethod.QR;
        }
        return gpsWithinRadius ? PresenceMethod.GPS : PresenceMethod.MANUAL;
    }
}
