package com.histar.be.common.gamification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void distance_samePoint_isZero() {
        assertTrue(GeoUtils.distanceMeters(11.143, 106.461, 11.143, 106.461) < 1);
    }

    @Test
    void distance_nearCuChi_within100m() {
        double distance = GeoUtils.distanceMeters(11.143, 106.461, 11.1435, 106.4615);
        assertTrue(distance < 100);
    }
}
