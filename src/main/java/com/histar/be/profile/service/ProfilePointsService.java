package com.histar.be.profile.service;

import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfilePointsService {

    public static final int XP_DISCOVERY = 10;
    public static final int XP_ARTIFACT_UNLOCK = 5;
    public static final int XP_CHECKIN = 30;

    private final ProfileRepository profileRepository;
    private final GamificationProperties gamificationProperties;

    @Transactional
    public int award(UUID userId, int points) {
        if (points <= 0) {
            return 0;
        }
        Profile profile = profileRepository.findById(userId).orElseThrow();
        int current = profile.getTotalPoints() == null ? 0 : profile.getTotalPoints();
        int next = current + points;
        profile.setTotalPoints(next);
        profile.setLevel(LevelCalculator.levelFromPoints(next, gamificationProperties.parseLevelThresholds()));
        profileRepository.save(profile);
        return points;
    }
}
