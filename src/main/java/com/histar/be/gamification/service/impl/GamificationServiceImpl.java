package com.histar.be.gamification.service.impl;

import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.badge.service.BadgeAwardService;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.gamification.GeoUtils;
import com.histar.be.common.gamification.QrPayload;
import com.histar.be.common.gamification.QrPayloadParser;
import com.histar.be.common.gamification.QrPayloadType;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.config.GamificationProperties;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.gamification.dto.BadgeEarnedDto;
import com.histar.be.gamification.dto.CheckinResultDto;
import com.histar.be.gamification.dto.HeritageOnsiteBonusResult;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.gamification.rules.UnlockRuleEvaluator;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.gamification.service.HeritageOnsiteBonusService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.quest.service.QuestProgressCompleter;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.secret.dto.SecretStoryResponse;
import com.histar.be.secret.entity.UserSecretUnlock;
import com.histar.be.secret.repository.UserSecretUnlockRepository;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import com.histar.be.visit.service.VisitSessionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    private final LocationService locationService;
    private final CheckinRepository checkinRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final UserSecretUnlockRepository userSecretUnlockRepository;
    private final BadgeAwardService badgeAwardService;
    private final GamificationProperties gamificationProperties;
    private final DiscoveryService discoveryService;
    private final ArtifactService artifactService;
    private final UnlockRuleEvaluator unlockRuleEvaluator;
    private final VisitSessionService visitSessionService;
    private final QuestCompletionService questCompletionService;
    private final QuestProgressCompleter questProgressCompleter;
    private final QuestRepository questRepository;
    private final HeritageOnsiteBonusService heritageOnsiteBonusService;

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

        if (payload.type() == QrPayloadType.SECRET) {
            boolean secretUnlocked = tryUnlockSecret(userId, locationId);
            return new CheckinResultDto(true, distance, List.of(), List.of(), secretUnlocked, 0);
        }

        return recordVisitAndMaybeReward(userId, locationId, latitude, longitude, distance);
    }

    @Override
    @Transactional
    public CheckinResultDto processDemoCheckin(UUID userId, UUID locationId) {
        Location location = locationService.findById(locationId);
        return recordVisitAndMaybeReward(
                userId, locationId, location.getLatitude(), location.getLongitude(), 0.0);
    }

    private CheckinResultDto recordVisitAndMaybeReward(
            UUID userId, UUID locationId, double latitude, double longitude, double distanceMeters) {
        boolean firstReward = !checkinRepository.existsByUserIdAndLocationId(userId, locationId);
        Checkin saved = checkinRepository.save(Checkin.builder()
                .userId(userId)
                .locationId(locationId)
                .latitude(latitude)
                .longitude(longitude)
                .createdAt(Instant.now())
                .build());
        visitSessionService.recordCheckinEvent(userId, locationId, saved.getId());

        List<QuestCompletedDto> questsCompleted =
                questCompletionService.tryComplete(userId, locationId, CompletionTrigger.CHECKIN);
        List<BadgeEarnedDto> allBadges = new ArrayList<>(badgeAwardService.evaluateAndAward(userId));
        for (QuestCompletedDto quest : questsCompleted) {
            allBadges.addAll(quest.badgesEarned());
        }

        if (firstReward) {
            applyFirstCheckinUnlocks(userId, locationId);
        }

        int bonusXp = 0;
        Optional<HeritageOnsiteBonusResult> onsiteBonus = heritageOnsiteBonusService.tryAward(userId, locationId);
        if (onsiteBonus.isPresent()) {
            bonusXp = onsiteBonus.get().xpAwarded();
            allBadges.addAll(onsiteBonus.get().badgesEarned());
        }

        List<UUID> questIds = questsCompleted.stream().map(QuestCompletedDto::questId).toList();
        return new CheckinResultDto(true, distanceMeters, questIds, dedupeBadges(allBadges), false, bonusXp);
    }

    private void applyFirstCheckinUnlocks(UUID userId, UUID locationId) {
        if (gamificationProperties.isRulesEngineEnabled()) {
            boolean rulesApplied = unlockRuleEvaluator.evaluateOnCheckin(userId, locationId);
            if (!rulesApplied) {
                log.warn("unlock_rules empty for location {} — falling back to legacy check-in unlock", locationId);
                applyLegacyCheckinUnlocks(userId, locationId);
            }
        } else {
            applyLegacyCheckinUnlocks(userId, locationId);
        }
    }

    private void applyLegacyCheckinUnlocks(UUID userId, UUID locationId) {
        discoveryService.recordOnCheckin(userId, locationId);
        artifactService.unlockOnCheckin(userId, locationId);
    }

    @Override
    @Transactional
    public QuestCompletedDto completeQuest(UUID userId, UUID questId) {
        return completeQuestIfInProgress(userId, questId)
                .orElseThrow(() -> new BusinessRuleException("Quest không ở trạng thái đang làm"));
    }

    @Override
    @Transactional
    public Optional<QuestCompletedDto> completeQuestIfInProgress(UUID userId, UUID questId) {
        return questProgressCompleter.completeIfInProgress(userId, questId);
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
        return badges.stream().distinct().toList();
    }
}
