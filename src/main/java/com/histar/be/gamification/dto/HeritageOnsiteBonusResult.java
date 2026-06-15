package com.histar.be.gamification.dto;

import java.util.List;

public record HeritageOnsiteBonusResult(int xpAwarded, List<BadgeEarnedDto> badgesEarned) {}
