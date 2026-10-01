package com.histar.be.gamification.service;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.gamification.dto.QuestProgressSnapshotDto;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.support.QuestDiscoveryProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EngagementOutcomeService {

    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final LocationService locationService;

    public QuestProgressSnapshotDto resolveQuestProgress(
            UUID userId, UUID locationId, String unlockKey, List<QuestCompletedDto> completedQuests) {
        if (locationId == null) {
            return null;
        }
        if (!completedQuests.isEmpty()) {
            QuestCompletedDto completed = completedQuests.get(0);
            Quest quest = questRepository.findById(completed.questId()).orElse(null);
            if (quest == null) {
                return null;
            }
            int total = QuestDiscoveryProgress.stepsTotal(quest);
            Location loc = locationService.findById(quest.getLocationId());
            return new QuestProgressSnapshotDto(
                    quest.getId(),
                    quest.getTitle(),
                    quest.getLocationId(),
                    loc.getName(),
                    true,
                    true,
                    total,
                    total,
                    completed.pointsAwarded());
        }

        for (Quest quest : questRepository.findByLocationId(locationId)) {
            var progressOpt = userQuestProgressRepository.findByUserIdAndQuestId(userId, quest.getId());
            if (progressOpt.isEmpty() || !QuestStatus.IN_PROGRESS.equals(progressOpt.get().getStatus())) {
                continue;
            }
            List<String> steps = QuestDiscoveryProgress.parseSteps(quest);
            if (!steps.contains(unlockKey)) {
                continue;
            }
            if (!userDiscoveryRepository.existsByUserIdAndLocationIdAndDiscoveryKey(
                    userId, locationId, unlockKey)) {
                continue;
            }
            var counts = QuestDiscoveryProgress.compute(
                    userId, locationId, quest, QuestStatus.IN_PROGRESS, userDiscoveryRepository);
            Location loc = locationService.findById(locationId);
            return new QuestProgressSnapshotDto(
                    quest.getId(),
                    quest.getTitle(),
                    locationId,
                    loc.getName(),
                    true,
                    false,
                    counts.currentStep(),
                    counts.stepsTotal(),
                    0);
        }
        return null;
    }
}
