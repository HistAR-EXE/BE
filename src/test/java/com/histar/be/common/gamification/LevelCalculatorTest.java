package com.histar.be.common.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LevelCalculatorTest {

    private static final int[] THRESHOLDS = {0, 100, 300, 700};

    @Test
    void level_atThresholds() {
        assertEquals(1, LevelCalculator.levelFromPoints(0, THRESHOLDS));
        assertEquals(2, LevelCalculator.levelFromPoints(100, THRESHOLDS));
        assertEquals(3, LevelCalculator.levelFromPoints(300, THRESHOLDS));
        assertEquals(4, LevelCalculator.levelFromPoints(700, THRESHOLDS));
    }

    @Test
    void levelName_explorer() {
        assertEquals("Explorer", LevelCalculator.info(50, THRESHOLDS).levelName());
    }
}
