package com.histar.be.gamification.service.impl;

import com.histar.be.badge.service.BadgeAwardService;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.gamification.GeoUtils;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.common.gamification.QrPayload;
import com.histar.be.common.gamification.QrPayloadParser;
import com.histar.be.common.gamification.QrPayloadType;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.config.GamificationProperties;
import com.histar.be.gamification.dto.BadgeEarnedDto;
import com.histar.be.gamification.dto.CheckinResultDto;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.secret.dto.SecretStoryResponse;
import com.histar.be.secret.entity.UserSecretUnlock;
import com.histar.be.secret.repository.UserSecretUnlockRepository;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final LocationService locationService;
    private final CheckinRepository checkinRepository;
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final ProfileRepository profileRepository;
    private final UserSecretUnlockRepository userSecretUnlockRepository;
    private final BadgeAwardService badgeAwardService;
    private final GamificationProperties gamificationProperties;

    @Override
    @Transactional
    public CheckinResultDto processCheckin(
            UUID userId, UUID locationId, double latitude, double longitude, String qrCode) {
        QrPayload payload = QrPayloadParser.parse(qrCode);
        if (!payload.locationId().equals(locationId)) {
            throw new BusinessRuleException("Mã QR không khớp địa điểm");
        }

        Location location = locationService.findById(locationId);
        double distance = GeoUtils.distanceMeters(
                latitude, longitude, location.getLatitude(), location.getLongitude());
        if (distance > gamificationProperties.getCheckinRadiusMeters()) {
            throw new BusinessRuleException(
                    "Bạn đang cách địa điểm quá xa (" + (int) distance + "m). Cần trong "
                            + gamificationProperties.getCheckinRadiusMeters() + "m");
        }

        boolean secretUnlocked = false;
        List<UUID> questsCompleted = new ArrayList<>();
        List<BadgeEarnedDto> allBadges = new ArrayList<>();

        if (payload.type() == QrPayloadType.SECRET) {
            secretUnlocked = tryUnlockSecret(userId, locationId);
            return new CheckinResultDto(true, distance, questsCompleted, allBadges, secretUnlocked);
        }

        checkinRepository.save(Checkin.builder()
                .userId(userId)
                .locationId(locationId)
                .latitude(latitude)
                .longitude(longitude)
                .createdAt(Instant.now())
                .build());

        List<Quest> quests = questRepository.findByLocationId(locationId);
        for (Quest quest : quests) {
            userQuestProgressRepository
                    .findByUserIdAndQuestId(userId, quest.getId())
                    .filter(p -> QuestStatus.IN_PROGRESS.equals(p.getStatus()))
                    .ifPresent(p -> {
                        QuestCompletedDto completed = completeQuest(userId, quest.getId());
                        questsCompleted.add(completed.questId());
                        allBadges.addAll(completed.badgesEarned());
                    });
        }

        allBadges.addAll(badgeAwardService.evaluateAndAward(userId));
        return new CheckinResultDto(true, distance, questsCompleted, dedupeBadges(allBadges), false);
    }

    @Override
    @Transactional
    public QuestCompletedDto completeQuest(UUID userId, UUID questId) {
        UserQuestProgress progress = userQuestProgressRepository
                .findByUserIdAndQuestId(userId, questId)
                .orElseThrow(() -> new BusinessRuleException("Quest chưa được bắt đầu"));
        if (!QuestStatus.IN_PROGRESS.equals(progress.getStatus())) {
            throw new BusinessRuleException("Quest không ở trạng thái đang làm");
        }

        Quest quest = questRepository.findById(questId).orElseThrow();
        progress.setStatus(QuestStatus.COMPLETED);
        progress.setCompletedAt(Instant.now());
        userQuestProgressRepository.save(progress);

        Profile profile = profileRepository.findById(userId).orElseThrow();
        int reward = quest.getPointsReward() == null ? 0 : quest.getPointsReward();
        int newPoints = (profile.getTotalPoints() == null ? 0 : profile.getTotalPoints()) + reward;
        profile.setTotalPoints(newPoints);
        profile.setLevel(LevelCalculator.levelFromPoints(newPoints, gamificationProperties.parseLevelThresholds()));
        profileRepository.save(profile);

        List<BadgeEarnedDto> badges = badgeAwardService.evaluateAndAward(userId);
        return new QuestCompletedDto(questId, reward, badges);
    }

    @Override
    @Transactional(readOnly = true)
    public SecretStoryResponse getSecretStory(UUID userId, UUID locationId) {
        locationService.findById(locationId);
        boolean unlocked = userSecretUnlockRepository.existsByUserIdAndLocationId(userId, locationId);
        if (!unlocked) {
            return new SecretStoryResponse(true, "Câu chuyện bí mật", null);
        }
        String story = questRepository.findByLocationId(locationId).stream()
                .findFirst()
                .map(Quest::getStory)
                .orElse(null);
        return new SecretStoryResponse(false, "Câu chuyện bí mật", story);
    }

    private boolean tryUnlockSecret(UUID userId, UUID locationId) {
        if (userSecretUnlockRepository.existsByUserIdAndLocationId(userId, locationId)) {
            return false;
        }
        boolean questDone = questRepository.findByLocationId(locationId).stream()
                .anyMatch(quest -> userQuestProgressRepository
                        .findByUserIdAndQuestId(userId, quest.getId())
                        .map(p -> QuestStatus.COMPLETED.equals(p.getStatus()))
                        .orElse(false));
        if (!questDone) {
            throw new BusinessRuleException("Hoàn thành quest tại địa điểm trước khi mở secret");
        }
        userSecretUnlockRepository.save(UserSecretUnlock.builder()
                .userId(userId)
                .locationId(locationId)
                .unlockedAt(Instant.now())
                .build());
        return true;
    }

    private List<BadgeEarnedDto> dedupeBadges(List<BadgeEarnedDto> badges) {
        return badges.stream()
                .distinct()
                .toList();
    }
}
