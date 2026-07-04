package com.histar.be.common.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevelCalculatorTest {

    private static final int[] THRESHOLDS = {0, 100, 300, 600, 1000};
    private static final List<String> NAMES = List.of(
            "Lữ hành mới",
            "Nhà khám phá",
            "Sử học gia",
            "Người gìn giữ di sản",
            "Huyền thoại lịch sử");

    @Test
    void level_atThresholds() {
        assertEquals(1, LevelCalculator.levelFromPoints(0, THRESHOLDS));
        assertEquals(2, LevelCalculator.levelFromPoints(100, THRESHOLDS));
        assertEquals(3, LevelCalculator.levelFromPoints(300, THRESHOLDS));
        assertEquals(4, LevelCalculator.levelFromPoints(600, THRESHOLDS));
        assertEquals(5, LevelCalculator.levelFromPoints(1000, THRESHOLDS));
    }

    @Test
    void levelName_luHanhMoi() {
        assertEquals("Lữ hành mới", LevelCalculator.info(50, THRESHOLDS, NAMES).levelName());
    }

    @Test
    void levelName_huyenThoai() {
        assertEquals("Huyền thoại lịch sử", LevelCalculator.info(1200, THRESHOLDS, NAMES).levelName());
    }
}
