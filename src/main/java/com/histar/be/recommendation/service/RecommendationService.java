package com.histar.be.recommendation.service;

import com.histar.be.recommendation.dto.RecommendationsResponse;
import java.util.UUID;

public interface RecommendationService {

    RecommendationsResponse forUser(UUID userId, UUID locationId, String afterUnlockKey);
}
