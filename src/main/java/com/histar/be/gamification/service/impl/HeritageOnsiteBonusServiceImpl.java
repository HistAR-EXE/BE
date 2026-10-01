package com.histar.be.gamification.service.impl;

import com.histar.be.badge.service.BadgeAwardService;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.config.GamificationProperties;
import com.histar.be.gamification.dto.HeritageOnsiteBonusResult;
import com.histar.be.gamification.entity.UserHeritageOnsiteBonus;
import com.histar.be.gamification.entity.UserHeritageOnsiteBonusId;
import com.histar.be.gamification.repository.UserHeritageOnsiteBonusRepository;
import com.histar.be.gamification.service.HeritageOnsiteBonusService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HeritageOnsiteBonusServiceImpl implements HeritageOnsiteBonusService {

    public static final int HERITAGE_ONSITE_BONUS_XP = 25;

    private final UserHeritageOnsiteBonusRepository bonusRepository;
    private final CheckinRepository checkinRepository;
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final ProfileRepository profileRepository;
    private final GamificationProperties gamificationProperties;
    private final BadgeAwardService badgeAwardService;

    @Override
    @Transactional
    public Optional<HeritageOnsiteBonusResult> tryAward(UUID userId, UUID locationId) {
        if (bonusRepository.existsByIdUserIdAndIdLocationId(userId, locationId)) {
            return Optional.empty();
        }
        if (!checkinRepository.existsByUserIdAndLocationId(userId, locationId)) {
            return Optional.empty();
        }
        if (!hasCompletedDiscoveryQuest(userId, locationId)) {
            return Optional.empty();
        }

        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return Optional.empty();
        }

        int newPoints =
                (profile.getTotalPoints() == null ? 0 : profile.getTotalPoints()) + HERITAGE_ONSITE_BONUS_XP;
        profile.setTotalPoints(newPoints);
        profile.setLevel(LevelCalculator.levelFromPoints(newPoints, gamificationProperties.parseLevelThresholds()));
        profileRepository.save(profile);

        bonusRepository.save(UserHeritageOnsiteBonus.builder()
                .id(new UserHeritageOnsiteBonusId(userId, locationId))
                .xpAwarded(HERITAGE_ONSITE_BONUS_XP)
                .awardedAt(Instant.now())
                .build());

        var badges = badgeAwardService.evaluateAndAward(userId);
        return Optional.of(new HeritageOnsiteBonusResult(HERITAGE_ONSITE_BONUS_XP, badges));
    }

    private boolean hasCompletedDiscoveryQuest(UUID userId, UUID locationId) {
        return questRepository.findByLocationId(locationId).stream()
                .filter(q -> "discovery".equalsIgnoreCase(q.getCompletionTrigger()))
                .anyMatch(q -> userQuestProgressRepository
                        .findByUserIdAndQuestId(userId, q.getId())
                        .map(p -> QuestStatus.COMPLETED.equals(p.getStatus()))
                        .orElse(false));
    }
}
