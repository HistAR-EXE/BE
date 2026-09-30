package com.histar.be.gamification.service.impl;

import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.badge.service.BadgeAwardService;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.presence.PresenceInput;
import com.histar.be.checkin.presence.PresenceMethod;
import com.histar.be.checkin.presence.PresenceScore;
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
import com.histar.be.gamification.dto.QuestProgressSnapshotDto;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import com.histar.be.gamification.rules.UnlockRuleEvaluator;
import com.histar.be.gamification.service.EngagementOutcomeService;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.gamification.service.HeritageOnsiteBonusService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.location.service.LocationUnlockService;
import com.histar.be.quest.service.QuestProgressCompleter;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.secret.dto.SecretStoryResponse;
import com.histar.be.stations.repository.StationRepository;
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
    private final ProfilePointsService profilePointsService;
    private final EngagementOutcomeService engagementOutcomeService;
    private final LocationUnlockService locationUnlockService;
    private final StationRepository stationRepository;

    @Override
    @Transactional
    public CheckinResultDto processCheckin(
            UUID userId, UUID locationId, double latitude, double longitude, String qrCode) {
        return processCheckin(userId, locationId, latitude, longitude, qrCode, PresenceInput.NONE);
    }

    @Override
    @Transactional
    public CheckinResultDto processCheckin(
            UUID userId,
            UUID locationId,
            Double latitude,
            Double longitude,
            String qrCode,
            PresenceInput presence) {
        PresenceInput input = presence != null ? presence : PresenceInput.NONE;

        QrPayload payload = null;
        if (qrCode != null && !qrCode.isBlank()) {
            payload = QrPayloadParser.parse(qrCode);
            if (!payload.locationId().equals(locationId)) {
                throw new BusinessRuleException("Mã QR không khớp địa điểm");
            }
        } else if (!input.qrVerified()) {
            throw new BusinessRuleException("Mã QR không hợp lệ");
        }

        Location location = locationService.findById(locationId);
        boolean gpsKnown = latitude != null && longitude != null;
        double distance = gpsKnown
                ? GeoUtils.distanceMeters(latitude, longitude, location.getLatitude(), location.getLongitude())
                : 0.0;
        boolean gpsWithinRadius = gpsKnown && distance <= gamificationProperties.getCheckinRadiusMeters();
        if (!gpsWithinRadius && !input.qrVerified()) {
            if (!gpsKnown) {
                throw new BusinessRuleException("Thiếu tọa độ GPS");
            }
            throw new BusinessRuleException(
                    "Bạn đang cách địa điểm quá xa (" + (int) distance + "m). Cần trong "
                            + gamificationProperties.getCheckinRadiusMeters() + "m");
        }

        if (input.clientUuid() != null) {
            Optional<Checkin> existing = checkinRepository.findByUserIdAndClientUuid(userId, input.clientUuid());
            if (existing.isPresent()) {
                Checkin prior = existing.get();
                return new CheckinResultDto(
                        true, distance, List.of(), List.of(), false, 0, 0, List.of(), null, List.of(),
                        prior.getPresenceScore(), prior.getPresenceMethod());
            }
        }

        if (payload != null && payload.type() == QrPayloadType.SECRET) {
            boolean secretUnlocked = tryUnlockSecret(userId, locationId);
            return new CheckinResultDto(true, distance, List.of(), List.of(), secretUnlocked, 0, 0, List.of(), null, List.of());
        }

        boolean sequenceOk = false;
        if (input.qrVerified() && input.stationCode() != null) {
            String siteForOrder = resolveSiteCode(locationId, input.siteCode());
            Integer previousOrder = checkinRepository
                    .findFirstByUserIdAndLocationIdAndPresenceMethodAndStationCodeIsNotNullOrderByCreatedAtDesc(
                            userId, locationId, PresenceMethod.QR.name())
                    .map(c -> stationSortOrder(siteForOrder, c.getStationCode()))
                    .orElse(null);
            sequenceOk = PresenceScore.isSequential(
                    previousOrder, stationSortOrder(siteForOrder, input.stationCode()));
        }
        int score = PresenceScore.compute(input.qrVerified(), gpsWithinRadius, sequenceOk);
        PresenceMethod method = PresenceScore.methodFor(input.qrVerified(), gpsWithinRadius);

        return recordVisitAndMaybeReward(
                userId,
                locationId,
                gpsKnown ? latitude : null,
                gpsKnown ? longitude : null,
                distance,
                input.stationCode(),
                input.clientUuid(),
                score,
                method);
    }

    /** stations.sort_order when the stations module knows the code; else trailing digits of the code. */
    private Integer stationSortOrder(String siteCode, String stationCode) {
        Integer fromTable = null;
        if (stationCode != null && !stationCode.isBlank()) {
            if (siteCode != null && !siteCode.isBlank()) {
                fromTable = stationRepository
                        .findBySiteCodeAndCodeAndActiveTrue(siteCode.trim(), stationCode.trim())
                        .map(s -> s.getSortOrder())
                        .orElse(null);
            }
            if (fromTable == null) {
                fromTable = stationRepository
                        .findFirstByCodeIgnoreCaseAndActiveTrueOrderBySortOrderAsc(stationCode.trim())
                        .map(s -> s.getSortOrder())
                        .orElse(null);
            }
        }
        return PresenceScore.resolveSortOrder(fromTable, stationCode);
    }

    private String resolveSiteCode(UUID locationId, String hint) {
        if (hint != null && !hint.isBlank()) {
            return hint.trim().toLowerCase();
        }
        try {
            return locationService.findById(locationId).getSiteCode();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    @Transactional
    public CheckinResultDto processDemoCheckin(UUID userId, UUID locationId) {
        Location location = locationService.findById(locationId);
        return recordVisitAndMaybeReward(
                userId,
                locationId,
                location.getLatitude(),
                location.getLongitude(),
                0.0,
                null,
                null,
                0,
                PresenceMethod.MANUAL);
    }

    private CheckinResultDto recordVisitAndMaybeReward(
            UUID userId,
            UUID locationId,
            Double latitude,
            Double longitude,
            double distanceMeters,
            String stationCode,
            UUID clientUuid,
            int presenceScore,
            PresenceMethod presenceMethod) {
        boolean firstLocationVisit = !checkinRepository.existsByUserIdAndLocationId(userId, locationId);
        boolean firstStationReward = stationCode != null
                && !stationCode.isBlank()
                && !checkinRepository.existsByUserIdAndLocationIdAndStationCodeIgnoreCase(
                        userId, locationId, stationCode.trim());
        // Location-level first visit (legacy quests) OR first check-in at this station within the site.
        boolean awardStationXp = firstStationReward || (stationCode == null && firstLocationVisit);
        Checkin saved = checkinRepository.save(Checkin.builder()
                .userId(userId)
                .locationId(locationId)
                .latitude(latitude)
                .longitude(longitude)
                .createdAt(Instant.now())
                .stationCode(stationCode)
                .presenceScore(presenceScore)
                .presenceMethod(presenceMethod.name())
                .clientUuid(clientUuid)
                .build());
        visitSessionService.recordCheckinEvent(userId, locationId, saved.getId());

        List<QuestCompletedDto> questsCompleted =
                questCompletionService.tryComplete(userId, locationId, CompletionTrigger.CHECKIN);
        List<BadgeEarnedDto> allBadges = new ArrayList<>(badgeAwardService.evaluateAndAward(userId));
        for (QuestCompletedDto quest : questsCompleted) {
            allBadges.addAll(quest.badgesEarned());
        }

        if (firstLocationVisit) {
            applyFirstCheckinUnlocks(userId, locationId);
        }

        int bonusXp = 0;
        Optional<HeritageOnsiteBonusResult> onsiteBonus = heritageOnsiteBonusService.tryAward(userId, locationId);
        if (onsiteBonus.isPresent()) {
            bonusXp = onsiteBonus.get().xpAwarded();
            allBadges.addAll(onsiteBonus.get().badgesEarned());
        }

        int xpPerStation = stationCode != null && !stationCode.isBlank()
                ? ProfilePointsService.XP_STATION
                : ProfilePointsService.XP_CHECKIN;
        int checkinXp = awardStationXp ? profilePointsService.award(userId, xpPerStation) : 0;
        // First location visit also awards the classic check-in XP once (on top of first station).
        if (firstLocationVisit && stationCode != null && !stationCode.isBlank()) {
            checkinXp += profilePointsService.award(userId, ProfilePointsService.XP_CHECKIN);
        }
        int xpEarned = checkinXp + bonusXp;
        List<UnlockedArtifactDto> newArtifacts = collectCheckinArtifacts(userId, locationId, firstLocationVisit);
        QuestProgressSnapshotDto questProgress =
                engagementOutcomeService.resolveQuestProgress(userId, locationId, "checkin", questsCompleted);
        var newlyUnlocked = locationUnlockService.resolveNewlyUnlocked(userId, questsCompleted);

        List<UUID> questIds = questsCompleted.stream().map(QuestCompletedDto::questId).toList();
        return new CheckinResultDto(
                true,
                distanceMeters,
                questIds,
                dedupeBadges(allBadges),
                false,
                bonusXp,
                xpEarned,
                newArtifacts,
                questProgress,
                newlyUnlocked,
                presenceScore,
                presenceMethod.name());
    }

    private List<UnlockedArtifactDto> collectCheckinArtifacts(UUID userId, UUID locationId, boolean firstReward) {
        if (!firstReward) {
            return List.of();
        }
        java.util.Map<java.util.UUID, UnlockedArtifactDto> collected = new java.util.LinkedHashMap<>();
        for (String key : List.of(
                "artifact:cuoc-chim",
                "artifact:chong-tre",
                "artifact:nap-ham",
                "artifact:den-dau",
                "artifact:khan-ran")) {
            artifactService.unlockByKeyCollecting(userId, key).forEach(a -> collected.putIfAbsent(a.id(), a));
        }
        return List.copyOf(collected.values());
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
