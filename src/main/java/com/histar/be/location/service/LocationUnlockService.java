package com.histar.be.location.service;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.location.dto.LocationResponse;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationUnlockService {

    private final LocationRepository locationRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;

    public boolean isUnlocked(UUID userId, Location location) {
        if (location.getUnlockPrerequisiteQuestId() == null) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return userQuestProgressRepository
                .findByUserIdAndQuestId(userId, location.getUnlockPrerequisiteQuestId())
                .map(p -> QuestStatus.COMPLETED.equals(p.getStatus()))
                .orElse(false);
    }

    public List<LocationResponse> resolveNewlyUnlocked(UUID userId, List<QuestCompletedDto> completedQuests) {
        if (userId == null || completedQuests == null || completedQuests.isEmpty()) {
            return List.of();
        }
        List<UUID> questIds = completedQuests.stream().map(QuestCompletedDto::questId).toList();
        List<Location> candidates = locationRepository.findByUnlockPrerequisiteQuestIdIn(questIds);
        List<LocationResponse> unlocked = new ArrayList<>();
        for (Location location : candidates) {
            if (isUnlocked(userId, location)) {
                unlocked.add(LocationResponse.from(location, null, true));
            }
        }
        return unlocked;
    }
}
