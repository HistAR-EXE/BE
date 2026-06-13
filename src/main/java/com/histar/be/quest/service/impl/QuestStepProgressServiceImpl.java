package com.histar.be.quest.service.impl;

import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.service.QuestStepProgressService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestStepProgressServiceImpl implements QuestStepProgressService {

    private final UserDiscoveryRepository userDiscoveryRepository;

    @Override
    @Transactional
    public void onDiscoveryRecorded(UUID userId, String unlockKey) {
        // Quest step progress is computed from user_discoveries on read (QuestDiscoveryProgress).
    }

    public static List<String> parseSteps(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static boolean allDiscoveryStepsDone(
            UUID userId, UUID locationId, List<String> steps, UserDiscoveryRepository repo) {
        if (locationId == null) {
            return false;
        }
        for (String key : steps) {
            if (!repo.existsByUserIdAndLocationIdAndDiscoveryKey(userId, locationId, key)) {
                return false;
            }
        }
        return true;
    }
}
