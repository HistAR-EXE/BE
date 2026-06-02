package com.histar.be.badge.service;

import com.histar.be.gamification.dto.BadgeEarnedDto;
import java.util.List;
import java.util.UUID;

public interface BadgeAwardService {

    List<BadgeEarnedDto> evaluateAndAward(UUID userId);
}
