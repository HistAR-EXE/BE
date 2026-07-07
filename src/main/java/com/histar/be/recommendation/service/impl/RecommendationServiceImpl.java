package com.histar.be.recommendation.service.impl;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.impl.QuestStepProgressServiceImpl;
import com.histar.be.recommendation.dto.RecommendationItem;
import com.histar.be.recommendation.dto.RecommendationsResponse;
import com.histar.be.recommendation.service.RecommendationService;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import com.histar.be.visit.entity.VisitSession;
import com.histar.be.visit.repository.VisitSessionRepository;
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

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final DiscoveryPointRepository discoveryPointRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final VisitSessionRepository visitSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public RecommendationsResponse forUser(UUID userId, UUID locationId, String afterUnlockKey) {
        List<DiscoveryPoint> points = discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId);
        Set<String> discovered = new HashSet<>();
        userDiscoveryRepository.findByUserIdAndLocationId(userId, locationId).forEach(d -> discovered.add(d.getDiscoveryKey()));

        // 1. TRUY VẤN MỤC TIÊU CÁ NHÂN HÓA (CHẨN JAVA FUNCTIONAL - ĐẢM BẢO BIẾN FINAL 100%)
        final String currentGoal = visitSessionRepository
                .findFirstByUserIdAndLocationIdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(userId, locationId, STATUS_ACTIVE)
                .map(VisitSession::getPersonaGoal)
                .filter(goal -> !goal.isBlank())
                .orElse("study"); // Nếu null hoặc trống thì lấy mặc định là "study"

        List<RecommendationItem> items = new ArrayList<>();

        // 2. GỢI Ý ĐIỂM TIẾP THEO DỰA TRÊN KHOẢNG CÁCH GẦN NHẤT (AFTER UNLOCK KEY)
        if (afterUnlockKey != null && !afterUnlockKey.isBlank()) {
            DiscoveryPoint current = points.stream()
                    .filter(p -> p.getUnlockKey().equals(afterUnlockKey)
                            || afterUnlockKey.contains(p.getUnlockKey()))
                    .findFirst()
                    .orElse(null);
            if (current != null) {
                final String goalPrefix = formatReasonPrefix(currentGoal);
                points.stream()
                        .filter(p -> !discovered.contains(p.getUnlockKey()))
                        .min(Comparator.comparingDouble(p -> distance(current, p)))
                        .ifPresent(next -> items.add(new RecommendationItem(
                                next.getUnlockKey(),
                                next.getName(),
                                goalPrefix + "Lộ trình tiếp nối sau " + current.getName(),
                                "/explore/" + locationId,
                                "personalized_after_" + currentGoal)));
            }
        }

        // 3. GỢI Ý HOÀN THIỆN TIẾN ĐỘ (PROGRESS GAP) NẾU CHƯA CÓ GỢI Ý LIỀN KỀ
        long missing = points.stream().filter(p -> !discovered.contains(p.getUnlockKey())).count();
        if (items.isEmpty() && missing > 0) {
            final String progressReason = formatProgressReason(currentGoal, points.size() - missing, points.size());
            points.stream()
                    .filter(p -> !discovered.contains(p.getUnlockKey()))
                    .min(Comparator.comparingDouble(p -> p.getMapXPct().doubleValue() + p.getMapYPct().doubleValue()))
                    .ifPresent(next -> items.add(new RecommendationItem(
                            next.getUnlockKey(),
                            next.getName(),
                            progressReason,
                            "/explore/" + locationId,
                            "personalized_gap_" + currentGoal)));
        }

        // 4. GỢI Ý ƯU TIÊN BƯỚC NHIỆM VỤ ĐANG THỰC HIỆN (IN_PROGRESS QUESTS)
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

                    String questReason = switch (currentGoal) {
                        case "study" -> "⭐ Nhiệm vụ trọng tâm lấy +XP: Còn thiếu bước này";
                        case "research" -> "🔍 Lộ trình khảo cứu: Đang thực hiện nhiệm vụ";
                        default -> "🎯 Nhiệm vụ khám phá đang mở — hoàn thành ngay";
                    };

                    items.add(new RecommendationItem(
                            key,
                            name,
                            questReason,
                            "/explore/" + locationId,
                            "quest_step_" + currentGoal));
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

    // --- CÁC HÀM TIỆN ÍCH TÙY BIẾN LÝ DO GỢI Ý THEO PERSONA GOAL ---

    private String formatReasonPrefix(String goal) {
        return switch (goal) {
            case "study" -> "⭐ Ôn tập & Tích lũy XP: ";
            case "research" -> "🔍 Khảo cứu tư liệu chuyên sâu: ";
            case "travel" -> "🎒 Điểm tham quan nổi bật: ";
            default -> "💡 Gợi ý tiếp theo: ";
        };
    }

    private String formatProgressReason(String goal, long discoveredCount, long total) {
        return switch (goal) {
            case "study" -> String.format("📚 Tiến độ học tập: %d/%d POI — Khám phá tiếp để tối đa XP!", discoveredCount, total);
            case "research" -> String.format("📜 Khảo cứu kiến trúc: Đã phân tích %d/%d điểm di tích", discoveredCount, total);
            case "travel" -> String.format("📸 Lộ trình check-in: Đã ghé thăm %d/%d tọa độ", discoveredCount, total);
            default -> String.format("Bạn đã khám phá %d/%d POI", discoveredCount, total);
        };
    }
}