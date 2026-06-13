package com.histar.be.recommendation.service.impl;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.impl.QuestStepProgressServiceImpl;
import com.histar.be.quest.support.QuestDiscoveryProgress;
import com.histar.be.recommendation.dto.RecommendationItem;
import com.histar.be.recommendation.dto.RecommendationsResponse;
import com.histar.be.recommendation.service.RecommendationService;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final DiscoveryPointRepository discoveryPointRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;

    @Override
    @Transactional(readOnly = true)
    public RecommendationsResponse forUser(UUID userId, UUID locationId, String afterUnlockKey) {
        List<DiscoveryPoint> points = discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId);
        Set<String> discovered = new HashSet<>();
        userDiscoveryRepository.findByUserIdAndLocationId(userId, locationId).forEach(d -> discovered.add(d.getDiscoveryKey()));

        List<RecommendationItem> items = new ArrayList<>();

        if (afterUnlockKey != null && !afterUnlockKey.isBlank()) {
            DiscoveryPoint current = points.stream()
                    .filter(p -> p.getUnlockKey().equals(afterUnlockKey)
                            || afterUnlockKey.contains(p.getUnlockKey()))
                    .findFirst()
                    .orElse(null);
            if (current != null) {
                points.stream()
                        .filter(p -> !discovered.contains(p.getUnlockKey()))
                        .min(Comparator.comparingDouble(p -> distance(current, p)))
                        .ifPresent(next -> items.add(new RecommendationItem(
                                next.getUnlockKey(),
                                next.getName(),
                                "Tiếp theo sau " + current.getName(),
                                "/explore/" + locationId,
                                "after_poi")));
            }
        }

        long missing = points.stream().filter(p -> !discovered.contains(p.getUnlockKey())).count();
        if (items.isEmpty() && missing > 0) {
            points.stream()
                    .filter(p -> !discovered.contains(p.getUnlockKey()))
                    .min(Comparator.comparingDouble(p -> p.getMapXPct().doubleValue() + p.getMapYPct().doubleValue()))
                    .ifPresent(next -> items.add(new RecommendationItem(
                            next.getUnlockKey(),
                            next.getName(),
                            "Bạn đã khám phá " + (points.size() - missing) + "/" + points.size() + " POI",
                            "/explore/" + locationId,
                            "progress_gap")));
        }

        for (Quest quest : questRepository.findByLocationId(locationId)) {
            var progress = userQuestProgressRepository.findByUserIdAndQuestId(userId, quest.getId());
            if (progress.isEmpty() || !QuestStatus.IN_PROGRESS.equals(progress.get().getStatus())) {
                continue;
            }
            var steps = QuestStepProgressServiceImpl.parseSteps(quest.getStepDiscoveryKeys());
            for (String key : steps) {
                if (!discovered.contains(key)) {
                    String name = points.stream()
                            .filter(p -> p.getUnlockKey().equals(key))
                            .map(DiscoveryPoint::getName)
                            .findFirst()
                            .orElse(key);
                    items.add(new RecommendationItem(
                            key,
                            name,
                            "Quest đang làm — còn thiếu bước này",
                            "/explore/" + locationId,
                            "quest_step"));
                    break;
                }
            }
        }

        return new RecommendationsResponse(locationId, items.stream().limit(3).toList());
    }

    private static double distance(DiscoveryPoint a, DiscoveryPoint b) {
        double dx = a.getMapXPct().doubleValue() - b.getMapXPct().doubleValue();
        double dy = a.getMapYPct().doubleValue() - b.getMapYPct().doubleValue();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
