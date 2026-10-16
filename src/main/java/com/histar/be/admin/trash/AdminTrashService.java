package com.histar.be.admin.trash;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.trash.AdminDeleteImpactResponse.ImpactItem;
import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.discovery.binding.repository.DiscoveryContentBindingRepository;
import com.histar.be.discovery.bridge.repository.ArtifactUnlockRequirementRepository;
import com.histar.be.discovery.bridge.repository.DiscoveryArtifactLinkRepository;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.lms.repository.AssignmentRepository;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTrashService {
    private final QuestRepository quests;
    private final DiscoveryPointRepository discoveryPoints;
    private final ArtifactRepository artifacts;
    private final NarrativeInvestigationRepository investigations;
    private final QuestCampaignRepository campaigns;
    private final UserQuestProgressRepository questProgress;
    private final AssignmentRepository assignments;
    private final StudyGroupRepository studyGroups;
    private final DiscoveryContentBindingRepository bindings;
    private final DiscoveryArtifactLinkRepository artifactLinks;
    private final ArtifactUnlockRequirementRepository unlockRequirements;
    private final UserDiscoveryRepository userDiscoveries;
    private final UserArtifactRepository userArtifacts;
    private final AdminAuditService audit;
    private final ProfileRepository profiles;

    @Transactional(readOnly = true)
    public PageResponse<AdminTrashItem> list(
            int page, int size, String resourceType, UUID locationId, String search, String sort) {
        List<AdminTrashItem> items = new ArrayList<>();
        discoveryPoints.findByDeletedAtIsNotNull().forEach(item -> items.add(toTrashItem(item)));
        artifacts.findByDeletedAtIsNotNull().forEach(item -> items.add(toTrashItem(item)));
        quests.findByDeletedAtIsNotNull().forEach(item -> items.add(toTrashItem(item)));

        String normalizedType = resourceType == null ? null : resourceType.trim().toUpperCase(Locale.ROOT);
        String normalizedSearch = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
        List<AdminTrashItem> filtered = items.stream()
                .filter(item -> normalizedType == null || normalizedType.isBlank()
                        || item.resourceType().equals(normalizedType))
                .filter(item -> locationId == null || locationId.equals(item.locationId()))
                .filter(item -> normalizedSearch == null || normalizedSearch.isBlank()
                        || contains(item.name(), normalizedSearch)
                        || contains(item.deleteReason(), normalizedSearch))
                .sorted(trashComparator(sort))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (filtered.size() + safeSize - 1) / safeSize;
        List<AdminTrashItem> pageItems = filtered.subList(from, to);
        List<UUID> deleterIds = pageItems.stream()
                .map(AdminTrashItem::deletedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Profile> deletersById = deleterIds.isEmpty()
                ? Map.of()
                : profiles.findAllById(deleterIds).stream()
                        .collect(Collectors.toMap(Profile::getId, Function.identity()));
        List<AdminTrashItem> resolved = pageItems.stream()
                .map(item -> withDeletedByName(item, deletersById))
                .toList();
        return PageResponse.<AdminTrashItem>builder()
                .items(resolved)
                .page(safePage)
                .size(safeSize)
                .totalItems(filtered.size())
                .totalPages(totalPages)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminDeleteImpactResponse discoveryPointImpact(UUID id) {
        DiscoveryPoint point = discoveryPoints.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discovery point not found: " + id));
        List<ImpactItem> blockers = new ArrayList<>();
        List<ImpactItem> warnings = new ArrayList<>();
        add(blockers, "CONTENT_BINDING",
                bindings.countByLocationIdAndUnlockKey(point.getLocationId(), point.getUnlockKey()),
                "Discovery point còn content binding đang hoạt động");
        add(blockers, "ARTIFACT_LINK",
                artifactLinks.countByLocationIdAndDiscoveryUnlockKey(point.getLocationId(), point.getUnlockKey()),
                "Discovery point còn liên kết mở artifact");
        add(blockers, "ARTIFACT_REQUIREMENT",
                unlockRequirements.countByLocationIdAndRequiredDiscoveryKey(point.getLocationId(), point.getUnlockKey()),
                "Discovery point đang là điều kiện mở artifact");
        long questReferences = quests.findByLocationId(point.getLocationId()).stream()
                .filter(quest -> containsKey(quest.getStepDiscoveryKeys(), point.getUnlockKey())
                        || containsKey(quest.getUnlockDiscoveryKeys(), point.getUnlockKey()))
                .count();
        add(blockers, "QUEST_REFERENCE", questReferences, "Discovery point đang được quest active tham chiếu");
        add(warnings, "LEARNER_HISTORY",
                userDiscoveries.countByLocationIdAndDiscoveryKey(point.getLocationId(), point.getUnlockKey()),
                "Lịch sử khám phá của người học được giữ nguyên");
        return impact("DISCOVERY_POINT", id, blockers, warnings);
    }

    @Transactional(readOnly = true)
    public AdminDeleteImpactResponse artifactImpact(UUID id) {
        Artifact artifact = artifacts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + id));
        List<ImpactItem> blockers = new ArrayList<>();
        List<ImpactItem> warnings = new ArrayList<>();
        add(blockers, "CONTENT_BINDING", bindings.countByArtifactId(id),
                "Artifact còn content binding đang hoạt động");
        add(blockers, "DISCOVERY_LINK",
                artifactLinks.countByLocationIdAndArtifactUnlockKey(artifact.getLocationId(), artifact.getUnlockKey()),
                "Artifact còn liên kết discovery đang hoạt động");
        add(blockers, "UNLOCK_REQUIREMENT",
                unlockRequirements.countByLocationIdAndArtifactUnlockKey(artifact.getLocationId(), artifact.getUnlockKey()),
                "Artifact còn điều kiện mở đang hoạt động");
        add(warnings, "LEARNER_HISTORY", userArtifacts.countByArtifactId(id),
                "Lịch sử artifact đã mở của người học được giữ nguyên");
        return impact("ARTIFACT", id, blockers, warnings);
    }

    @Transactional(readOnly = true)
    public AdminDeleteImpactResponse questImpact(UUID id) {
        Quest quest = quests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + id));
        Instant now = Instant.now();
        List<ImpactItem> blockers = new ArrayList<>();
        List<ImpactItem> warnings = new ArrayList<>();
        add(blockers, "ACTIVE_PILOT", investigations.findByQuestIdAndEnabledTrue(id).isPresent() ? 1 : 0,
                "Quest còn pilot active");
        add(blockers, "ACTIVE_CAMPAIGN",
                campaigns.countByQuestIdAndEnabledTrueAndStartsAtLessThanEqualAndEndsAtGreaterThan(id, now, now),
                "Quest còn campaign đang hoạt động");
        add(blockers, "ACTIVE_ASSIGNMENT",
                assignments.countByQuestIdAndDueAtIsNull(id) + assignments.countByQuestIdAndDueAtGreaterThanEqual(id, now),
                "Quest còn assignment chưa hết hạn");
        add(blockers, "ACTIVE_STUDY_GROUP", studyGroups.countByQuestIdAndExpiresAtGreaterThan(id, now),
                "Quest còn study group chưa hết hạn");
        add(blockers, "DEPENDENT_QUEST", quests.findByUnlockAfterQuestIdAndDeletedAtIsNull(id).size(),
                "Quest còn quest active phụ thuộc");
        add(warnings, "IN_PROGRESS", questProgress.countByQuestIdAndStatusIgnoreCase(id, "IN_PROGRESS"),
                "Progress dở dang sẽ được giữ lại nhưng quest không còn hiển thị");
        return impact("QUEST", id, blockers, warnings);
    }

    @Transactional
    public void softDeleteQuest(UUID questId, UUID actorId, AdminDeleteRequest request) {
        String reason = normalizeReason(request);
        var quest = lockQuestWithParent(questId, false);
        if (quest.getDeletedAt() != null) {
            return;
        }
        requireVersion(quest.getRowVersion(), request.rowVersion());
        quests.findActiveChildrenForUpdate(questId);
        ensureDeletable(questImpact(questId));
        Long oldVersion = quest.getRowVersion();
        quest.setDeletedAt(Instant.now());
        quest.setDeletedBy(actorId);
        quest.setDeleteReason(reason);
        quests.saveAndFlush(quest);
        audit.append(new AdminAuditCommand(actorId, "QUEST", questId, "SOFT_DELETED", oldVersion,
                quest.getRowVersion(), reason, request.idempotencyKey(),
                Map.of("title", quest.getTitle() == null ? "" : quest.getTitle())));
    }

    @Transactional
    public void restoreQuest(UUID questId, UUID actorId, AdminRestoreRequest request) {
        var quest = lockQuestWithParent(questId, true);
        if (quest.getDeletedAt() == null) {
            return;
        }
        requireVersion(quest.getRowVersion(), request.rowVersion());
        Long oldVersion = quest.getRowVersion();
        quest.setDeletedAt(null);
        quest.setDeletedBy(null);
        quest.setDeleteReason(null);
        try {
            quests.saveAndFlush(quest);
        } catch (DataIntegrityViolationException exception) {
            throw restoreConflict();
        }
        audit.append(new AdminAuditCommand(actorId, "QUEST", questId, "RESTORED", oldVersion,
                quest.getRowVersion(), null, request.idempotencyKey(),
                Map.of("title", quest.getTitle() == null ? "" : quest.getTitle())));
    }

    @Transactional
    public void softDeleteDiscoveryPoint(UUID id, UUID actorId, AdminDeleteRequest request) {
        DiscoveryPoint point = discoveryPoints.findIncludingDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discovery point not found: " + id));
        if (point.getDeletedAt() != null) return;
        requireVersion(point.getRowVersion(), request.rowVersion());
        ensureDeletable(discoveryPointImpact(id));
        String reason = normalizeReason(request);
        Long oldVersion = point.getRowVersion();
        point.setDeletedAt(Instant.now());
        point.setDeletedBy(actorId);
        point.setDeleteReason(reason);
        discoveryPoints.saveAndFlush(point);
        audit.append(new AdminAuditCommand(actorId, "DISCOVERY_POINT", id, "SOFT_DELETED", oldVersion,
                point.getRowVersion(), reason, request.idempotencyKey(), Map.of("name", safe(point.getName()))));
    }

    @Transactional
    public void restoreDiscoveryPoint(UUID id, UUID actorId, AdminRestoreRequest request) {
        DiscoveryPoint point = discoveryPoints.findIncludingDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discovery point not found: " + id));
        if (point.getDeletedAt() == null) return;
        requireVersion(point.getRowVersion(), request.rowVersion());
        Long oldVersion = point.getRowVersion();
        point.setDeletedAt(null);
        point.setDeletedBy(null);
        point.setDeleteReason(null);
        try {
            discoveryPoints.saveAndFlush(point);
        } catch (DataIntegrityViolationException exception) {
            throw restoreConflict();
        }
        audit.append(new AdminAuditCommand(actorId, "DISCOVERY_POINT", id, "RESTORED", oldVersion,
                point.getRowVersion(), null, request.idempotencyKey(), Map.of("name", safe(point.getName()))));
    }

    @Transactional
    public void softDeleteArtifact(UUID id, UUID actorId, AdminDeleteRequest request) {
        Artifact artifact = artifacts.findIncludingDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + id));
        if (artifact.getDeletedAt() != null) return;
        requireVersion(artifact.getRowVersion(), request.rowVersion());
        ensureDeletable(artifactImpact(id));
        String reason = normalizeReason(request);
        Long oldVersion = artifact.getRowVersion();
        artifact.setDeletedAt(Instant.now());
        artifact.setDeletedBy(actorId);
        artifact.setDeleteReason(reason);
        artifacts.saveAndFlush(artifact);
        audit.append(new AdminAuditCommand(actorId, "ARTIFACT", id, "SOFT_DELETED", oldVersion,
                artifact.getRowVersion(), reason, request.idempotencyKey(), Map.of("name", safe(artifact.getName()))));
    }

    @Transactional
    public void restoreArtifact(UUID id, UUID actorId, AdminRestoreRequest request) {
        Artifact artifact = artifacts.findIncludingDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + id));
        if (artifact.getDeletedAt() == null) return;
        requireVersion(artifact.getRowVersion(), request.rowVersion());
        Long oldVersion = artifact.getRowVersion();
        artifact.setDeletedAt(null);
        artifact.setDeletedBy(null);
        artifact.setDeleteReason(null);
        try {
            artifacts.saveAndFlush(artifact);
        } catch (DataIntegrityViolationException exception) {
            throw restoreConflict();
        }
        audit.append(new AdminAuditCommand(actorId, "ARTIFACT", id, "RESTORED", oldVersion,
                artifact.getRowVersion(), null, request.idempotencyKey(), Map.of("name", safe(artifact.getName()))));
    }

    private String normalizeReason(AdminDeleteRequest request) {
        String preset = request.reasonPreset() == null ? "" : request.reasonPreset().trim();
        if (!java.util.Set.of("Sai nguồn", "Lỗi kỹ thuật", "Trùng nội dung", "Yêu cầu pháp lý", "Khác").contains(preset)) {
            throw new BusinessRuleException("Lý do xóa không hợp lệ");
        }
        String detail = request.reasonDetail() == null ? "" : request.reasonDetail().trim();
        if ("Khác".equals(preset) && detail.length() < 10) {
            throw new BusinessRuleException("Lý do khác cần ít nhất 10 ký tự");
        }
        return "Khác".equals(preset) ? detail : preset;
    }

    private Quest lockQuestWithParent(UUID questId, boolean requireActiveParent) {
        Quest snapshot = quests.findIncludingDeletedById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + questId));
        if (snapshot.getUnlockAfterQuestId() != null) {
            Quest parent = quests.findIncludingDeletedByIdForUpdate(snapshot.getUnlockAfterQuestId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent quest not found: " + snapshot.getUnlockAfterQuestId()));
            if (requireActiveParent && parent.getDeletedAt() != null) {
                throw new NarrativeConflictException(
                        ErrorCode.PARENT_DELETED, "Quest cha đã bị xóa; hãy khôi phục quest cha trước");
            }
        }
        return quests.findIncludingDeletedByIdForUpdate(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + questId));
    }

    private void requireVersion(Long current, Long requested) {
        if (requested == null || !Objects.equals(current, requested)) {
            throw new ConflictException("Nội dung đã bị thay đổi; hãy tải lại trước khi thao tác");
        }
    }

    private void ensureDeletable(AdminDeleteImpactResponse impact) {
        if (!impact.deletable()) {
            throw new BusinessRuleException(impact.blockers().get(0).message());
        }
    }

    private AdminDeleteImpactResponse impact(
            String resourceType, UUID id, List<ImpactItem> blockers, List<ImpactItem> warnings) {
        return new AdminDeleteImpactResponse(resourceType, id, blockers.isEmpty(), List.copyOf(blockers), List.copyOf(warnings));
    }

    private void add(List<ImpactItem> items, String code, long count, String message) {
        if (count > 0) items.add(new ImpactItem(code, count, message));
    }

    private boolean containsKey(String raw, String key) {
        if (raw == null || raw.isBlank() || key == null) return false;
        String normalized = raw.replace("[", "").replace("]", "").replace("\"", "");
        for (String token : normalized.split("[,\\r\\n]")) {
            if (key.equals(token.trim())) return true;
        }
        return false;
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private Comparator<AdminTrashItem> trashComparator(String sort) {
        String value = sort == null || sort.isBlank() ? "deletedAt,desc" : sort;
        String[] parts = value.split(",", 2);
        Comparator<AdminTrashItem> comparator = switch (parts[0]) {
            case "name" -> Comparator.comparing(AdminTrashItem::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "resourceType" -> Comparator.comparing(AdminTrashItem::resourceType);
            default -> Comparator.comparing(AdminTrashItem::deletedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? comparator : comparator.reversed();
    }

    private AdminTrashItem toTrashItem(DiscoveryPoint point) {
        return new AdminTrashItem("DISCOVERY_POINT", point.getId(), point.getLocationId(), point.getName(),
                point.getDeletedAt(), point.getDeletedBy(), point.getDeleteReason(), point.getRowVersion(), null);
    }

    private AdminTrashItem toTrashItem(Artifact artifact) {
        return new AdminTrashItem("ARTIFACT", artifact.getId(), artifact.getLocationId(), artifact.getName(),
                artifact.getDeletedAt(), artifact.getDeletedBy(), artifact.getDeleteReason(), artifact.getRowVersion(),
                null);
    }

    private AdminTrashItem toTrashItem(Quest quest) {
        return new AdminTrashItem("QUEST", quest.getId(), quest.getLocationId(), quest.getTitle(),
                quest.getDeletedAt(), quest.getDeletedBy(), quest.getDeleteReason(), quest.getRowVersion(), null);
    }

    private AdminTrashItem withDeletedByName(AdminTrashItem item, Map<UUID, Profile> deletersById) {
        Profile deleter = item.deletedBy() == null ? null : deletersById.get(item.deletedBy());
        String name = deleter == null ? null : deleter.getDisplayName();
        return new AdminTrashItem(
                item.resourceType(),
                item.id(),
                item.locationId(),
                item.name(),
                item.deletedAt(),
                item.deletedBy(),
                item.deleteReason(),
                item.rowVersion(),
                name);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private NarrativeConflictException restoreConflict() {
        return new NarrativeConflictException(
                ErrorCode.RESTORE_CONFLICT, "Không thể khôi phục vì giá trị duy nhất đã được sử dụng");
    }
}
