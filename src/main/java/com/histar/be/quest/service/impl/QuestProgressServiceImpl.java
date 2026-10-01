package com.histar.be.quest.service.impl;

import com.histar.be.analytics.service.AnalyticsEventService;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.dto.QuestProgressResponse;
import com.histar.be.quest.dto.QuestResponse;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.quest.service.QuestService;
import com.histar.be.quest.support.QuestDiscoveryProgress;
import com.histar.be.organization.service.TrialEntitlementGuard;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestProgressServiceImpl implements QuestProgressService {

    private final QuestService questService;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final QuestCompletionService questCompletionService;
    private final AnalyticsEventService analyticsEventService;
    private final VisitSessionRepository visitSessionRepository;
    private final CheckinRepository checkinRepository;
    private final TrialEntitlementGuard trialEntitlementGuard;

    @Override
    @Transactional(readOnly = true)
    public Page<QuestResponse> listByLocation(UUID locationId, Pageable pageable) {
        List<QuestResponse> quests = (locationId == null ? questService.findAll() : questService.findByLocationId(locationId))
                .stream().map(QuestResponse::from).toList();
        return toPage(quests, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestProgressResponse> listMyQuests(UUID userId, UUID locationId, String status, Pageable pageable) {
        List<QuestProgressResponse> items = (locationId == null ? questService.findAll() : questService.findByLocationId(locationId))
                .stream()
                .map(quest -> toProgress(userId, quest))
                .filter(progress -> status == null || status.isBlank() || status.equals(progress.status()))
                .toList();
        return toPage(items, pageable);
    }

    @Override
    @Transactional
    public QuestProgressResponse startQuest(UUID userId, UUID questId) {
        trialEntitlementGuard.assertStudentCanWrite(userId);
        Quest quest = questService.findById(questId);
        UserQuestProgress progress = userQuestProgressRepository
                .findByUserIdAndQuestId(userId, questId)
                .orElseGet(() -> UserQuestProgress.builder()
                        .userId(userId)
                        .questId(questId)
                        .status(QuestStatus.NOT_STARTED)
                        .build());

        if (QuestStatus.COMPLETED.equals(progress.getStatus())) {
            throw new BusinessRuleException("Quest đã hoàn thành");
        }
        if (QuestStatus.IN_PROGRESS.equals(progress.getStatus())) {
            var counts = QuestDiscoveryProgress.compute(
                    userId, quest.getLocationId(), quest, QuestStatus.IN_PROGRESS, userDiscoveryRepository);
            if (progress.getCurrentStep() == null) {
                progress.setCurrentStep(counts.currentStep());
                progress.setStepsTotal(counts.stepsTotal());
                userQuestProgressRepository.save(progress);
            }
            questCompletionService.tryComplete(userId, quest.getLocationId(), CompletionTrigger.START_QUEST);
            return toProgress(userId, quest);
        }
        if (!QuestStatus.NOT_STARTED.equals(progress.getStatus())) {
            throw new BusinessRuleException("Không thể bắt đầu quest từ trạng thái hiện tại");
        }

        progress.setStatus(QuestStatus.IN_PROGRESS);
        progress.setLocationId(quest.getLocationId());
        progress.setStartedAt(Instant.now());
        var counts = QuestDiscoveryProgress.compute(
                userId, quest.getLocationId(), quest, QuestStatus.IN_PROGRESS, userDiscoveryRepository);
        progress.setCurrentStep(counts.currentStep());
        progress.setStepsTotal(counts.stepsTotal());
        userQuestProgressRepository.save(progress);

        UUID sessionId = visitSessionRepository
                .findFirstByUserIdAndLocationIdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(
                        userId, quest.getLocationId(), "ACTIVE")
                .map(s -> s.getId())
                .orElse(null);
        analyticsEventService.recordQuestStarted(userId, quest.getLocationId(), questId, sessionId);

        questCompletionService.tryComplete(userId, quest.getLocationId(), CompletionTrigger.START_QUEST);
        return toProgress(userId, quest);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestProgressResponse getProgress(UUID userId, UUID questId) {
        Quest quest = questService.findById(questId);
        return toProgress(userId, quest);
    }

    private QuestProgressResponse toProgress(UUID userId, Quest quest) {
        String status = userQuestProgressRepository
                .findByUserIdAndQuestId(userId, quest.getId())
                .map(UserQuestProgress::getStatus)
                .orElse(QuestStatus.NOT_STARTED);
        UserQuestProgress progress = userQuestProgressRepository
                .findByUserIdAndQuestId(userId, quest.getId())
                .orElse(null);
        var counts = QuestDiscoveryProgress.compute(
                userId, quest.getLocationId(), quest, status, userDiscoveryRepository);
        var steps = QuestDiscoveryProgress.parseSteps(quest);
        boolean discoveryStepsComplete = !steps.isEmpty()
                && QuestDiscoveryProgress.countDone(
                                userId, quest.getLocationId(), steps, userDiscoveryRepository)
                        >= steps.size();
        boolean hasCheckinAtLocation = quest.getLocationId() != null
                && checkinRepository.existsByUserIdAndLocationId(userId, quest.getLocationId());
        boolean requireOnsite = Boolean.TRUE.equals(quest.getRequireOnsiteCheckin())
                || QuestDiscoveryProgress.requiresCheckinStep(quest);
        return new QuestProgressResponse(
                quest.getId(),
                quest.getLocationId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStory(),
                quest.getPointsReward(),
                status,
                counts.currentStep(),
                counts.stepsTotal(),
                discoveryStepsComplete,
                hasCheckinAtLocation,
                quest.getCompletionTrigger(),
                requireOnsite,
                progress != null ? progress.getStartedAt() : null,
                progress != null ? progress.getCompletedAt() : null);
    }

    private <T> Page<T> toPage(List<T> items, Pageable pageable) {
        int start = Math.min((int) pageable.getOffset(), items.size());
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }
}
