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
import com.histar.be.quest.repository.QuestRepository;
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
    private final QuestRepository questRepository;

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
            if (!meetsCondition(badge, userId, questCompletedCount, checkinCount, totalPoints)) {
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

    private boolean meetsCondition(
            Badge badge, UUID userId, long questCompletedCount, long checkinCount, int totalPoints) {
        String type = badge.getConditionType();
        int required = badge.getConditionValue() == null ? 0 : badge.getConditionValue();
        if (type == null) {
            return false;
        }
        return switch (type) {
            case "quest_complete" -> questCompletedCount >= required;
            case "points" -> totalPoints >= required;
            case "checkin" -> checkinCount >= required;
            case "heritage_onsite" -> meetsHeritageOnsite(badge, userId);
            default -> false;
        };
    }

    private boolean meetsHeritageOnsite(Badge badge, UUID userId) {
        UUID locationId = badge.getLocationId();
        if (locationId == null) {
            return false;
        }
        if (!checkinRepository.existsByUserIdAndLocationId(userId, locationId)) {
            return false;
        }
        return questRepository.findByLocationId(locationId).stream()
                .filter(q -> "discovery".equalsIgnoreCase(q.getCompletionTrigger()))
                .anyMatch(q -> userQuestProgressRepository
                        .findByUserIdAndQuestId(userId, q.getId())
                        .map(p -> QuestStatus.COMPLETED.equals(p.getStatus()))
                        .orElse(false));
    }
}
