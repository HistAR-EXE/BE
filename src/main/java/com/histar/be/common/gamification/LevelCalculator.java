package com.histar.be.common.gamification;

import java.util.List;

public final class LevelCalculator {

    private LevelCalculator() {}

    public static int levelFromPoints(int totalPoints, int[] thresholds) {
        return levelFromPoints(totalPoints, thresholds, thresholds.length);
    }

    public static int levelFromPoints(int totalPoints, int[] thresholds, int maxLevel) {
        int level = 1;
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (totalPoints >= thresholds[i]) {
                level = i + 1;
                break;
            }
        }
        return Math.min(level, maxLevel);
    }

    public static LevelInfo info(int totalPoints, int[] thresholds, List<String> levelNames) {
        int maxLevel = Math.min(thresholds.length, levelNames.size());
        int level = levelFromPoints(totalPoints, thresholds, maxLevel);
        String levelName = levelNames.get(Math.min(level - 1, levelNames.size() - 1));
        int currentThreshold = thresholds[level - 1];
        int nextThreshold = level < thresholds.length ? thresholds[level] : thresholds[thresholds.length - 1];
        int pointsToNext = level < maxLevel ? Math.max(0, nextThreshold - totalPoints) : 0;
        int span = nextThreshold - currentThreshold;
        int progress = span > 0 ? (int) Math.min(100, ((totalPoints - currentThreshold) * 100L) / span) : 100;
        if (level >= maxLevel) {
            progress = 100;
            pointsToNext = 0;
        }
        return new LevelInfo(level, levelName, pointsToNext, progress);
    }
}
