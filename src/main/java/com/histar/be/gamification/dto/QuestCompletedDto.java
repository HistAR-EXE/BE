package com.histar.be.gamification.dto;

import java.util.List;
import java.util.UUID;

public record QuestCompletedDto(UUID questId, int pointsAwarded, List<BadgeEarnedDto> badgesEarned) {}
