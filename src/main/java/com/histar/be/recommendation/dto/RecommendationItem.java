package com.histar.be.recommendation.dto;

public record RecommendationItem(
        String unlockKey, String poiName, String reason, String deepLink, String triggerType) {}
