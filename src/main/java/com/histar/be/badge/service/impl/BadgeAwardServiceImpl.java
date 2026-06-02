package com.histar.be.badge.service.impl;

import com.histar.be.badge.entity.Badge;
import com.histar.be.badge.repository.BadgeRepository;
import com.histar.be.badge.service.BadgeAwardService;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.gamification.dto.BadgeEarnedDto;
import com.histar.be.userbadge.entity.UserBadge;
import com.histar.be.userbadge.repository.UserBadgeRepository;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeAwardServiceImpl implements BadgeAwardService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final CheckinRepository checkinRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public List<BadgeEarnedDto> evaluateAndAward(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return List.of();
        }
        long questCompletedCount =
                userQuestProgressRepository.countByUserIdAndStatus(userId, QuestStatus.COMPLETED);
        long checkinCount = checkinRepository.countByUserId(userId);
        int totalPoints = profile.getTotalPoints() == null ? 0 : profile.getTotalPoints();

        List<BadgeEarnedDto> earned = new ArrayList<>();
        for (Badge badge : badgeRepository.findAll()) {
            if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                continue;
            }
            if (!meetsCondition(badge, questCompletedCount, checkinCount, totalPoints)) {
                continue;
            }
            userBadgeRepository.save(UserBadge.builder()
                    .userId(userId)
                    .badgeId(badge.getId())
                    .earnedAt(Instant.now())
                    .build());
            earned.add(new BadgeEarnedDto(badge.getId(), badge.getName(), badge.getIconUrl()));
        }
        return earned;
    }

    private boolean meetsCondition(Badge badge, long questCompletedCount, long checkinCount, int totalPoints) {
        String type = badge.getConditionType();
        int required = badge.getConditionValue() == null ? 0 : badge.getConditionValue();
        if (type == null) {
            return false;
        }
        return switch (type) {
            case "quest_complete" -> questCompletedCount >= required;
            case "points" -> totalPoints >= required;
            case "checkin" -> checkinCount >= required;
            default -> false;
        };
    }
}
