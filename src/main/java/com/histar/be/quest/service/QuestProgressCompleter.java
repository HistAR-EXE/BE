package com.histar.be.quest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.analytics.dto.QuestCompletionMetadata;
import com.histar.be.analytics.service.AnalyticsEventService;
import com.histar.be.badge.service.BadgeAwardService;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.config.GamificationProperties;
import com.histar.be.gamification.dto.BadgeEarnedDto;
import com.histar.be.gamification.dto.HeritageOnsiteBonusResult;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.gamification.service.HeritageOnsiteBonusService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.support.QuestDiscoveryProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QuestProgressCompleter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserQuestProgressRepository userQuestProgressRepository;
    private final QuestRepository questRepository;
    private final ProfileRepository profileRepository;
    private final GamificationProperties gamificationProperties;
    private final BadgeAwardService badgeAwardService;
    private final AnalyticsEventService analyticsEventService;
    private final VisitSessionRepository visitSessionRepository;
    private final CheckinRepository checkinRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final HeritageOnsiteBonusService heritageOnsiteBonusService;
    private final com.histar.be.lms.service.LmsAssignmentService lmsAssignmentService;

    @Transactional
    public Optional<QuestCompletedDto> completeIfInProgress(UUID userId, UUID questId) {
        return completeIfInProgress(userId, questId, null);
    }

    @Transactional
    public Optional<QuestCompletedDto> completeIfInProgress(
            UUID userId, UUID questId, CompletionTrigger trigger) {
        Quest quest = questRepository.findById(questId).orElseThrow();
        Instant now = Instant.now();
        int updated = userQuestProgressRepository.markCompletedIfInProgress(
                userId, questId, QuestStatus.COMPLETED, QuestStatus.IN_PROGRESS, now);
        if (updated == 0) {
            return Optional.empty();
        }

        Profile profile = profileRepository.findById(userId).orElseThrow();
        int reward = quest.getPointsReward() == null ? 0 : quest.getPointsReward();
        int newPoints = (profile.getTotalPoints() == null ? 0 : profile.getTotalPoints()) + reward;
        profile.setTotalPoints(newPoints);
        profile.setLevel(LevelCalculator.levelFromPoints(newPoints, gamificationProperties.parseLevelThresholds()));
        profileRepository.save(profile);

        UUID sessionId = resolveActiveSessionId(userId, quest.getLocationId());
        String metadataJson = buildCompletionMetadata(userId, quest, trigger);
        analyticsEventService.recordQuestCompleted(
                userId, quest.getLocationId(), questId, sessionId, metadataJson);

        List<BadgeEarnedDto> badges = new ArrayList<>(badgeAwardService.evaluateAndAward(userId));
        if (quest.getLocationId() != null && "discovery".equalsIgnoreCase(quest.getCompletionTrigger())) {
            heritageOnsiteBonusService
                    .tryAward(userId, quest.getLocationId())
                    .map(HeritageOnsiteBonusResult::badgesEarned)
                    .ifPresent(badges::addAll);
        }
        lmsAssignmentService.autoGradeOnQuestCompletion(userId, questId);
        return Optional.of(new QuestCompletedDto(questId, reward, dedupeBadges(badges)));
    }

    private List<BadgeEarnedDto> dedupeBadges(List<BadgeEarnedDto> badges) {
        return badges.stream().distinct().toList();
    }

    private String buildCompletionMetadata(UUID userId, Quest quest, CompletionTrigger trigger) {
        if (trigger == null) {
            return null;
        }
        var steps = QuestDiscoveryProgress.parseSteps(quest);
        int stepsDone = QuestDiscoveryProgress.countDone(
                userId, quest.getLocationId(), steps, userDiscoveryRepository);
        boolean hasCheckin = checkinRepository.existsByUserIdAndLocationId(userId, quest.getLocationId());
        var metadata = new QuestCompletionMetadata(
                quest.getId(),
                trigger.name(),
                quest.getLocationId(),
                stepsDone,
                steps.size(),
                hasCheckin);
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private UUID resolveActiveSessionId(UUID userId, UUID locationId) {
        if (locationId == null) {
            return null;
        }
        return visitSessionRepository
                .findFirstByUserIdAndLocationIdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(
                        userId, locationId, "ACTIVE")
                .map(s -> s.getId())
                .orElse(null);
    }
}
