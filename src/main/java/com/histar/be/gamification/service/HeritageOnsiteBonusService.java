package com.histar.be.gamification.service;

import com.histar.be.gamification.dto.HeritageOnsiteBonusResult;
import java.util.Optional;
import java.util.UUID;

public interface HeritageOnsiteBonusService {

    Optional<HeritageOnsiteBonusResult> tryAward(UUID userId, UUID locationId);
}
