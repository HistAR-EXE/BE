package com.histar.be.config;

import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gamification")
public class GamificationProperties {

    private int checkinRadiusMeters = 100;
    private String levelThresholds = "0,100,300,700";
    private boolean rulesEngineEnabled = false;

    public int getCheckinRadiusMeters() {
        return checkinRadiusMeters;
    }

    public void setCheckinRadiusMeters(int checkinRadiusMeters) {
        this.checkinRadiusMeters = checkinRadiusMeters;
    }

    public String getLevelThresholds() {
        return levelThresholds;
    }

    public void setLevelThresholds(String levelThresholds) {
        this.levelThresholds = levelThresholds;
    }

    public int[] parseLevelThresholds() {
        return Arrays.stream(levelThresholds.split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    public boolean isRulesEngineEnabled() {
        return rulesEngineEnabled;
    }

    public void setRulesEngineEnabled(boolean rulesEngineEnabled) {
        this.rulesEngineEnabled = rulesEngineEnabled;
    }
}
