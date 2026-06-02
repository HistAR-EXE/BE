package com.histar.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "viral")
public class ViralProperties {

    private int shareBonusPoints = 15;
    private String shareCaption = "Khám phá di sản Việt cùng TimeLens! #TimeLens #DiSanVietNam";
    private int leaderboardLimit = 50;
    private int leaderboardCacheSeconds = 60;

    public int getShareBonusPoints() {
        return shareBonusPoints;
    }

    public void setShareBonusPoints(int shareBonusPoints) {
        this.shareBonusPoints = shareBonusPoints;
    }

    public String getShareCaption() {
        return shareCaption;
    }

    public void setShareCaption(String shareCaption) {
        this.shareCaption = shareCaption;
    }

    public int getLeaderboardLimit() {
        return leaderboardLimit;
    }

    public void setLeaderboardLimit(int leaderboardLimit) {
        this.leaderboardLimit = leaderboardLimit;
    }

    public int getLeaderboardCacheSeconds() {
        return leaderboardCacheSeconds;
    }

    public void setLeaderboardCacheSeconds(int leaderboardCacheSeconds) {
        this.leaderboardCacheSeconds = leaderboardCacheSeconds;
    }
}
