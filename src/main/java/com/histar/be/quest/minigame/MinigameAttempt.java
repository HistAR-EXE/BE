package com.histar.be.quest.minigame;

import java.util.List;

public record MinigameAttempt(
        String levelId,
        List<Drop> drops,
        List<Tap> taps,
        List<Double> angles,
        Integer durationMs,
        Integer count,
        Integer quietMs,
        Double baselineRms,
        Double peakRms,
        Integer holdMs,
        String transcript) {

    public MinigameAttempt(
            String levelId,
            List<Drop> drops,
            List<Tap> taps,
            List<Double> angles,
            Integer durationMs) {
        this(levelId, drops, taps, angles, durationMs, null, null, null, null, null, null);
    }

    public record Drop(String itemId, String floorId) {}

    public record Tap(double x, double y, int atMs) {}
}
