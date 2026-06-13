package com.histar.be.recommendation.dto;

import java.util.List;
import java.util.UUID;

public record RecommendationsResponse(UUID locationId, List<RecommendationItem> items) {}
