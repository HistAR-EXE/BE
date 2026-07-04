package com.histar.be.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gamification")
public class GamificationProperties {

    private int checkinRadiusMeters = 100;
    private String levelThresholds = "0,100,300,600,1000";
    private String levelNames =
            "Lữ hành mới,Nhà khám phá,Sử học gia,Người gìn giữ di sản,Huyền thoại lịch sử";
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

    public String getLevelNames() {
        return levelNames;
    }

    public void setLevelNames(String levelNames) {
        this.levelNames = levelNames;
    }

    public List<String> parseLevelNames() {
        return Arrays.stream(levelNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
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
