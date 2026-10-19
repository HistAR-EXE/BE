package com.histar.be.quest.v2;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.dto.QuestRewardSummary;
import com.histar.be.quest.dto.QuestStartOutcome;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.rules.QuestFactsLoader;
import com.histar.be.quest.rules.QuestRules;
import com.histar.be.quest.rules.QuestStepDefs;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.quest.service.QuestVisibility;
import com.histar.be.quest.v2.dto.QuestV2Card;
import com.histar.be.quest.v2.dto.QuestV2Detail;
import com.histar.be.quest.v2.dto.QuestV2Lock;
import com.histar.be.quest.v2.dto.QuestV2LockKey;
import com.histar.be.quest.v2.dto.QuestV2Progress;
import com.histar.be.quest.v2.dto.QuestV2QuestRef;
import com.histar.be.quest.v2.dto.QuestV2StartResponse;
import com.histar.be.quest.v2.dto.QuestV2Step;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.repository.StationRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhiệm vụ v2: BE tính trạng thái nhiệm vụ, lý do khóa và trạng thái từng bước; FE chỉ hiển thị.
 * Mọi dữ liệu của một di tích được nạp một lần (không N+1).
 */
@Service
@RequiredArgsConstructor
public class QuestStateService {

    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String LOCK_AFTER_QUEST = "AFTER_QUEST";
    public static final String LOCK_DISCOVERY_KEYS = "DISCOVERY_KEYS";

    private final QuestRepository questRepository;
    private final QuestStepRepository questStepRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final DiscoveryPointRepository discoveryPointRepository;
    private final StationRepository stationRepository;
    private final LocationRepository locationRepository;
    private final QuestFactsLoader questFactsLoader;
    private final QuestVisibility questVisibility;
    private final QuestProgressService questProgressService;
    private final QuestCompletionService questCompletionService;

    /** Danh sách nhiệm vụ của một di tích, lọc theo nhóm (ONLINE/ONSITE) nếu có. */
    @Transactional(readOnly = true)
    public List<QuestV2Card> list(UUID locationId, String track, UUID userId) {
        if (locationId == null) {
            throw new BusinessRuleException("locationId là bắt buộc");
        }
        String wanted = parseTrack(track);
        List<Quest> all = visibleSorted(questRepository.findByLocationId(locationId));
        List<Quest> quests = wanted == null
                ? all
                : all.stream().filter(q -> wanted.equals(QuestRules.normalizeTrack(q.getTrack()))).toList();
        Context ctx = context(locationId, userId, all);
        return quests.stream().map(q -> card(q, ctx)).toList();
    }

    @Transactional(readOnly = true)
    public QuestV2Detail detail(UUID questId, UUID userId) {
        Quest quest = load(questId);
        Context ctx = context(quest.getLocationId(), userId, questRepository.findByLocationId(quest.getLocationId()));
        return detail(quest, ctx);
    }

    /** Nhận nhiệm vụ (BE kiểm tra điều kiện mở). Đủ bước bắt buộc từ trước thì hoàn thành ngay. */
    @Transactional
    public QuestV2StartResponse start(UUID questId, UUID userId) {
        QuestStartOutcome outcome = questProgressService.startQuestWithOutcome(userId, questId);
        Quest quest = load(questId);
        Context ctx = context(quest.getLocationId(), userId, questRepository.findByLocationId(quest.getLocationId()));
        return new QuestV2StartResponse(detail(quest, ctx), outcome.completed());
    }

    /** Hoàn thành các nhiệm vụ đang làm đã đủ bước bắt buộc (sau khi luật thay đổi). Gọi nhiều lần vẫn an toàn. */
    @Transactional
    public List<QuestCompletedDto> reconcile(UUID locationId, UUID userId) {
        if (locationId == null || userId == null) {
            return List.of();
        }
        return questCompletionService.tryComplete(userId, locationId, CompletionTrigger.START_QUEST);
    }

    // ------------------------------------------------------------------ dựng DTO

    private QuestV2Card card(Quest quest, Context ctx) {
        QuestRules.Evaluation ev = QuestRules.evaluate(stepDefs(quest, ctx), ctx.facts());
        UserQuestProgress progress = ctx.progress().get(quest.getId());
        String stored = progress == null ? null : progress.getStatus();
        boolean completed = QuestStatus.COMPLETED.equals(stored);
        boolean inProgress = QuestStatus.IN_PROGRESS.equals(stored);
        QuestV2Lock lock = completed || inProgress ? null : lockFor(quest, ctx);
        String status = completed
                ? STATUS_COMPLETED
                : inProgress ? STATUS_IN_PROGRESS : lock != null ? STATUS_LOCKED : STATUS_AVAILABLE;
        return new QuestV2Card(
                quest.getId(),
                quest.getLocationId(),
                QuestRules.normalizeTrack(quest.getTrack()),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStory(),
                quest.getCoverImage(),
                quest.getSortOrder() == null ? 100 : quest.getSortOrder(),
                quest.getDifficulty(),
                quest.getEstimatedMinutes(),
                quest.getPointsReward() == null ? 0 : quest.getPointsReward(),
                status,
                lock,
                new QuestV2Progress(ev.requiredDone(), ev.requiredTotal(), ev.optionalDone(), ev.optionalTotal()),
                completed ? null : ev.nextStepId(),
                progress == null ? null : progress.getStartedAt(),
                progress == null ? null : progress.getCompletedAt());
    }

    private QuestV2Detail detail(Quest quest, Context ctx) {
        QuestV2Card card = card(quest, ctx);
        List<QuestStep> rows = ctx.steps().getOrDefault(quest.getId(), List.of());
        Map<UUID, QuestStep> rowById = rows.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(QuestStep::getId, Function.identity(), (a, b) -> a));
        QuestRules.Evaluation ev = QuestRules.evaluate(stepDefs(quest, ctx), ctx.facts());
        UserQuestProgress progress = ctx.progress().get(quest.getId());
        var startedAt = progress == null ? null : progress.getStartedAt();
        List<QuestV2Step> steps = ev.steps().stream().map(result -> {
            QuestRules.StepDef def = result.step();
            QuestStep row = rowById.get(def.id());
            return new QuestV2Step(
                    def.id(),
                    def.order(),
                    def.key(),
                    def.actionType(),
                    stepTitle(def, row, ctx),
                    row == null ? null : row.getObjective(),
                    row == null ? null : row.getDescription(),
                    row == null ? null : row.getHint(),
                    row == null ? null : row.getActionLabel(),
                    row == null ? null : row.getPreviewImage(),
                    row == null ? null : row.getPortalEra(),
                    row == null ? null : row.getMinigameId(),
                    row == null ? null : row.getChatPrompt(),
                    def.required(),
                    def.premium(),
                    result.state().name(),
                    result.doneAt(),
                    QuestRules.isPreDiscovered(result.doneAt(), startedAt));
        }).toList();
        return new QuestV2Detail(card, steps, QuestRewardSummary.from(quest));
    }

    private QuestV2Lock lockFor(Quest quest, Context ctx) {
        UUID afterId = quest.getUnlockAfterQuestId();
        if (afterId != null) {
            UserQuestProgress after = ctx.progress().get(afterId);
            boolean done = after != null && QuestStatus.COMPLETED.equals(after.getStatus());
            if (!done) {
                Quest afterQuest = ctx.questsById().get(afterId);
                if (afterQuest == null) {
                    afterQuest = questRepository.findById(afterId).orElse(null);
                }
                String title = afterQuest == null || afterQuest.getTitle() == null ? "nhiệm vụ trước" : afterQuest.getTitle();
                String afterStatus = after == null || after.getStatus() == null
                        ? "NOT_STARTED"
                        : after.getStatus().toUpperCase(Locale.ROOT);
                return new QuestV2Lock(
                        LOCK_AFTER_QUEST,
                        "Hoàn thành “" + title + "” để mở",
                        new QuestV2QuestRef(afterId, title, afterStatus),
                        List.of());
            }
        }
        List<String> keys = QuestRules.parseKeys(quest.getUnlockDiscoveryKeys());
        if (!keys.isEmpty()) {
            List<QuestV2LockKey> items = keys.stream()
                    .map(k -> new QuestV2LockKey(
                            k, ctx.pointLabels().getOrDefault(k, k), ctx.facts().discoveries().containsKey(k)))
                    .toList();
            long done = items.stream().filter(QuestV2LockKey::done).count();
            if (done < items.size()) {
                return new QuestV2Lock(
                        LOCK_DISCOVERY_KEYS,
                        "Khám phá đủ " + items.size() + " điểm để mở (" + done + "/" + items.size() + ")",
                        null,
                        items);
            }
        }
        return null;
    }

    private String stepTitle(QuestRules.StepDef def, QuestStep row, Context ctx) {
        String key = def.key() == null ? "" : def.key().trim();
        if (key.startsWith(QuestRules.STATION_PREFIX)) {
            String code = key.substring(QuestRules.STATION_PREFIX.length()).trim().toUpperCase(Locale.ROOT);
            String name = ctx.stationNames().get(code);
            if (name != null && !name.isBlank()) {
                // Tên trạm lấy từ bảng stations để đội nội dung đổi tên mà không phải sửa nhiệm vụ.
                return "Trạm " + code + " · " + name;
            }
        }
        if (row != null && row.getTitle() != null && !row.getTitle().isBlank()) {
            return row.getTitle();
        }
        if (QuestRules.ACTION_CHECKIN.equalsIgnoreCase(def.actionType())) {
            return "Check-in tại di tích";
        }
        return ctx.pointLabels().getOrDefault(key, key);
    }

    // ------------------------------------------------------------------ nạp dữ liệu

    private record Context(
            QuestRules.Facts facts,
            Map<UUID, UserQuestProgress> progress,
            Map<UUID, List<QuestStep>> steps,
            Map<String, String> pointLabels,
            Map<String, String> stationNames,
            Map<UUID, Quest> questsById) {}

    private Context context(UUID locationId, UUID userId, Collection<Quest> quests) {
        QuestRules.Facts facts = questFactsLoader.load(userId, locationId);
        Map<UUID, UserQuestProgress> progress = userId == null
                ? Map.of()
                : userQuestProgressRepository.findByUserId(userId).stream()
                        .filter(p -> p.getQuestId() != null)
                        .collect(Collectors.toMap(UserQuestProgress::getQuestId, Function.identity(), (a, b) -> a));
        List<UUID> questIds = quests.stream().map(Quest::getId).filter(Objects::nonNull).toList();
        Map<UUID, List<QuestStep>> steps = questIds.isEmpty()
                ? Map.of()
                : questStepRepository.findByQuestIdIn(questIds).stream()
                        .filter(s -> s.getQuest() != null && s.getQuest().getId() != null)
                        .collect(Collectors.groupingBy(s -> s.getQuest().getId()));
        Map<String, String> pointLabels = new HashMap<>();
        if (locationId != null) {
            for (DiscoveryPoint point : discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId)) {
                if (point.getUnlockKey() != null && point.getName() != null) {
                    pointLabels.putIfAbsent(point.getUnlockKey(), point.getName());
                }
            }
        }
        Map<String, String> stationNames = new HashMap<>();
        String siteCode = locationId == null
                ? null
                : locationRepository.findById(locationId).map(l -> l.getSiteCode()).orElse(null);
        if (siteCode != null && !siteCode.isBlank()) {
            for (Station station : stationRepository.findBySiteCodeAndActiveTrueOrderBySortOrderAsc(siteCode)) {
                if (station.getCode() != null) {
                    stationNames.put(station.getCode().trim().toUpperCase(Locale.ROOT), station.getName());
                }
            }
        }
        Map<UUID, Quest> byId = quests.stream()
                .filter(q -> q.getId() != null)
                .collect(Collectors.toMap(Quest::getId, Function.identity(), (a, b) -> a));
        return new Context(facts, progress, steps, pointLabels, stationNames, byId);
    }

    private List<QuestRules.StepDef> stepDefs(Quest quest, Context ctx) {
        return QuestStepDefs.forQuest(quest, ctx.steps().getOrDefault(quest.getId(), List.of()));
    }

    private Quest load(UUID questId) {
        return questRepository.findById(questId)
                .filter(questVisibility::visible)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
    }

    private List<Quest> visibleSorted(List<Quest> quests) {
        return quests.stream()
                .filter(questVisibility::visible)
                // Online trước, Tại di tích sau; trong mỗi nhóm theo sort_order rồi tên.
                .sorted(Comparator.comparing((Quest q) -> QuestRules.TRACK_ONSITE.equals(QuestRules.normalizeTrack(q.getTrack())))
                        .thenComparing(q -> q.getSortOrder() == null ? 100 : q.getSortOrder())
                        .thenComparing(q -> q.getTitle() == null ? "" : q.getTitle()))
                .toList();
    }

    private static String parseTrack(String track) {
        if (track == null || track.isBlank()) {
            return null;
        }
        String value = track.trim().toUpperCase(Locale.ROOT);
        if (!QuestRules.TRACK_ONLINE.equals(value) && !QuestRules.TRACK_ONSITE.equals(value)) {
            throw new BusinessRuleException("track phải là ONLINE hoặc ONSITE");
        }
        return value;
    }
}
