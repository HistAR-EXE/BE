package com.histar.be.analytics.service;

import com.histar.be.analytics.dto.AdminAnalyticsOverviewResponse;
import com.histar.be.analytics.dto.SessionReplayResponse;
import java.util.UUID;

public interface AdminAnalyticsService {

    AdminAnalyticsOverviewResponse overview(UUID locationId);

    SessionReplayResponse sessionReplay(UUID sessionId);
}
