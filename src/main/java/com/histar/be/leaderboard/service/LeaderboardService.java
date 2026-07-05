package com.histar.be.leaderboard.service;

import com.histar.be.leaderboard.dto.LeaderboardResponse;
import java.util.UUID;

public interface LeaderboardService {

    LeaderboardResponse getLeaderboard(String scope, String city, UUID currentUserId, UUID groupId);
}
