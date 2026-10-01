package com.histar.be.chat.service.impl;

import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.chat.service.PlayerStoryContextService;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.support.QuestDiscoveryProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.visit.repository.VisitSessionEventRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerStoryContextServiceImpl implements PlayerStoryContextService {

    private final DiscoveryPointRepository discoveryPointRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final ArtifactRepository artifactRepository;
    private final UserArtifactRepository userArtifactRepository;
    private final VisitSessionRepository visitSessionRepository;
    private final VisitSessionEventRepository visitSessionEventRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> build(UUID userId, UUID locationId) {
        return build(userId, locationId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> build(UUID userId, UUID locationId, String currentUnlockKey) {
        List<DiscoveryPoint> points = discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId);
        Set<String> discovered = new HashSet<>();
        userDiscoveryRepository.findByUserIdAndLocationId(userId, locationId).forEach(d -> discovered.add(d.getDiscoveryKey()));

        List<String> discoveredPois = new ArrayList<>();
        List<String> missingPois = new ArrayList<>();
        DiscoveryPoint currentPoint = null;
        for (DiscoveryPoint point : points) {
            if (discovered.contains(point.getUnlockKey())) {
                discoveredPois.add(point.getName());
            } else {
                missingPois.add(point.getName());
            }
            if (currentUnlockKey != null && currentUnlockKey.equals(point.getUnlockKey())) {
                currentPoint = point;
            }
        }

        String lastVisitedPoi = resolveLastVisitedPoi(userId, locationId, points);

        List<String> completedQuests = new ArrayList<>();
        Map<String, Object> questState = null;
        for (Quest quest : questRepository.findByLocationId(locationId)) {
            var progressOpt = userQuestProgressRepository.findByUserIdAndQuestId(userId, quest.getId());
            if (progressOpt.isPresent() && QuestStatus.COMPLETED.equals(progressOpt.get().getStatus())) {
                completedQuests.add(quest.getTitle());
            }
            if (progressOpt.isPresent() && QuestStatus.IN_PROGRESS.equals(progressOpt.get().getStatus())) {
                var counts = QuestDiscoveryProgress.compute(
                        userId, locationId, quest, QuestStatus.IN_PROGRESS, userDiscoveryRepository);
                questState = Map.of(
                        "title", quest.getTitle(),
                        "currentStep", counts.currentStep(),
                        "stepsTotal", counts.stepsTotal());
            }
        }

        List<String> missingArtifacts = new ArrayList<>();
        Set<UUID> unlockedIds = new HashSet<>();
        userArtifactRepository.findByUserId(userId).forEach(ua -> unlockedIds.add(ua.getArtifactId()));
        for (Artifact artifact : artifactRepository.findByLocationIdOrderBySortOrder(locationId)) {
            if (!unlockedIds.contains(artifact.getId())) {
                missingArtifacts.add(artifact.getName());
            }
        }

        List<String> nearbyPois = new ArrayList<>();
        final DiscoveryPoint currentPoi = currentPoint;
        if (currentPoi != null) {
            points.stream()
                    .filter(p -> !p.getUnlockKey().equals(currentPoi.getUnlockKey()))
                    .sorted(Comparator.comparingDouble(p -> distance(currentPoi, p)))
                    .limit(3)
                    .forEach(p -> nearbyPois.add(p.getName()));
        }

        String nextSuggested = missingPois.isEmpty() ? null : missingPois.get(0);

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("discoveredPois", discoveredPois);
        ctx.put("missingPoiNames", missingPois);
        ctx.put("discoveredPoiNames", discoveredPois);
        ctx.put("discoveredCount", discoveredPois.size());
        ctx.put("totalPoi", points.size());
        ctx.put("completedQuests", completedQuests);
        ctx.put("favoriteTopics", List.of());
        ctx.put("lastVisitedPoi", lastVisitedPoi);
        ctx.put("currentPoi", currentPoint != null ? currentPoint.getName() : null);
        ctx.put("nearbyPois", nearbyPois);
        ctx.put("questState", questState);
        ctx.put("missingArtifacts", missingArtifacts.subList(0, Math.min(5, missingArtifacts.size())));
        ctx.put("nextSuggestedPoi", nextSuggested);
        ctx.put(
                "storyGuideHint",
                missingPois.isEmpty()
                        ? "Bạn đã khám phá hết các điểm chính — hãy check-in tại chỗ để hoàn tất hành trình."
                        : "Nếu em còn thời gian, chị gợi ý em ghé " + nextSuggested + "...");
        if (!missingArtifacts.isEmpty()) {
            ctx.put(
                    "artifactHint",
                    "Em còn thiếu " + missingArtifacts.size() + " cổ vật nữa để hoàn thành bộ sưu tập.");
        }
        return ctx;
    }

    private String resolveLastVisitedPoi(UUID userId, UUID locationId, List<DiscoveryPoint> points) {
        Map<String, String> keyToName = new HashMap<>();
        points.forEach(p -> keyToName.put(p.getUnlockKey(), p.getName()));
        return visitSessionRepository.findByUserIdAndLocationIdOrderByStartedAtDesc(userId, locationId).stream()
                .findFirst()
                .flatMap(session -> visitSessionEventRepository
                        .findByVisitSessionIdOrderByCreatedAtAsc(session.getId())
                        .stream()
                        .filter(e -> "discovery".equals(e.getEventType()))
                        .reduce((a, b) -> b)
                        .map(e -> keyToName.getOrDefault(e.getEventKey(), e.getEventKey())))
                .orElseGet(() -> userDiscoveryRepository.findByUserIdAndLocationId(userId, locationId).stream()
                        .max(Comparator.comparing(UserDiscovery::getDiscoveredAt))
                        .map(UserDiscovery::getDiscoveryKey)
                        .map(k -> keyToName.getOrDefault(k, k))
                        .orElse(null));
    }

    private static double distance(DiscoveryPoint a, DiscoveryPoint b) {
        double dx = a.getMapXPct().doubleValue() - b.getMapXPct().doubleValue();
        double dy = a.getMapYPct().doubleValue() - b.getMapYPct().doubleValue();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
