package com.histar.be.common.gamification;

import java.util.List;

public final class LevelCalculator {

    private static final List<String> LEVEL_NAMES =
            List.of("Explorer", "Time Traveler", "History Hunter", "Legend");

    private LevelCalculator() {}

    public static int levelFromPoints(int totalPoints, int[] thresholds) {
        int level = 1;
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (totalPoints >= thresholds[i]) {
                level = i + 1;
                break;
            }
        }
        return Math.min(level, LEVEL_NAMES.size());
    }

    public static LevelInfo info(int totalPoints, int[] thresholds) {
        int level = levelFromPoints(totalPoints, thresholds);
        String levelName = LEVEL_NAMES.get(level - 1);
        int currentThreshold = thresholds[level - 1];
        int nextThreshold = level < thresholds.length ? thresholds[level] : thresholds[thresholds.length - 1];
        int pointsToNext = level < thresholds.length ? Math.max(0, nextThreshold - totalPoints) : 0;
        int span = nextThreshold - currentThreshold;
        int progress = span > 0 ? (int) Math.min(100, ((totalPoints - currentThreshold) * 100L) / span) : 100;
        if (level >= thresholds.length) {
            progress = 100;
            pointsToNext = 0;
        }
        return new LevelInfo(level, levelName, pointsToNext, progress);
    }
}
