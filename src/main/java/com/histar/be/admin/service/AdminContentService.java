package com.histar.be.admin.service;

import com.histar.be.admin.dto.AdminArtifactRequest;
import com.histar.be.admin.dto.AdminArtifactResponse;
import com.histar.be.admin.dto.AdminDiscoveryPointRequest;
import com.histar.be.admin.dto.AdminDiscoveryPointResponse;
import com.histar.be.admin.dto.AdminQuestRequest;
import com.histar.be.admin.dto.AdminQuestResponse;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.hotspot.entity.Hotspot;
import com.histar.be.hotspot.repository.HotspotRepository;
import com.histar.be.location.service.LocationEraValidationService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminContentService {

    private final DiscoveryPointRepository discoveryPointRepository;
    private final ArtifactRepository artifactRepository;
    private final QuestRepository questRepository;
    private final LocationEraValidationService locationEraValidationService;
    private final HotspotRepository hotspotRepository;
    private final AdminAuditService audit;
    private final CurrentUserAccessor currentUser;

    public List<AdminDiscoveryPointResponse> listDiscoveryPoints(UUID locationId) {
        List<DiscoveryPoint> points = locationId != null
                ? discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId)
                : discoveryPointRepository.findByDeletedAtIsNull();
        return points.stream().map(this::toDiscoveryResponse).toList();
    }

    @Transactional
    public AdminDiscoveryPointResponse createDiscoveryPoint(AdminDiscoveryPointRequest request) {
        DiscoveryPoint saved = discoveryPointRepository.saveAndFlush(DiscoveryPoint.builder()
                .locationId(request.locationId())
                .name(request.name())
                .mapXPct(request.mapXPct())
                .mapYPct(request.mapYPct())
                .unlockKey(request.unlockKey())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build());
        syncSceneHotspotAngles(request.unlockKey(), request.yaw(), request.pitch());
        audit.append(new AdminAuditCommand(actorId(), "DISCOVERY_POINT", saved.getId(), "CREATED", null,
                saved.getRowVersion(), null, null, Map.of("name", saved.getName())));
        log.info("admin create discovery-point id={} locationId={}", saved.getId(), saved.getLocationId());
        return toDiscoveryResponse(saved);
    }

    @Transactional
    public AdminDiscoveryPointResponse updateDiscoveryPoint(UUID id, AdminDiscoveryPointRequest request) {
        DiscoveryPoint point = discoveryPointRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discovery point not found: " + id));
        requireVersion(point.getRowVersion(), request.rowVersion());
        Long oldVersion = point.getRowVersion();
        point.setLocationId(request.locationId());
        point.setName(request.name());
        point.setMapXPct(request.mapXPct());
        point.setMapYPct(request.mapYPct());
        point.setUnlockKey(request.unlockKey());
        if (request.sortOrder() != null) {
            point.setSortOrder(request.sortOrder());
        }
        DiscoveryPoint saved = discoveryPointRepository.saveAndFlush(point);
        syncSceneHotspotAngles(request.unlockKey(), request.yaw(), request.pitch());
        audit.append(new AdminAuditCommand(actorId(), "DISCOVERY_POINT", id, "UPDATED", oldVersion,
                saved.getRowVersion(), null, null, Map.of("name", saved.getName())));
        return toDiscoveryResponse(saved);
    }

    private AdminDiscoveryPointResponse toDiscoveryResponse(DiscoveryPoint point) {
        Double yaw = null;
        Double pitch = null;
        UUID sceneId = parseScenePanoramaId(point.getUnlockKey());
        if (sceneId != null) {
            List<Hotspot> inbound = hotspotRepository.findByContentRefAndType(sceneId.toString(), "scene");
            if (!inbound.isEmpty()) {
                yaw = inbound.get(0).getYaw();
                pitch = inbound.get(0).getPitch();
            }
        }
        return AdminDiscoveryPointResponse.from(point, yaw, pitch);
    }

    /**
     * When admin sets yaw/pitch on a scene discovery unlockKey {@code scene:{panoramaId}},
     * update inbound scene-link hotspots that navigate to that panorama.
     */
    private void syncSceneHotspotAngles(String unlockKey, Double yaw, Double pitch) {
        if (yaw == null && pitch == null) {
            return;
        }
        UUID sceneId = parseScenePanoramaId(unlockKey);
        if (sceneId == null) {
            return;
        }
        List<Hotspot> inbound = hotspotRepository.findByContentRefAndType(sceneId.toString(), "scene");
        for (Hotspot hotspot : inbound) {
            if (yaw != null) {
                hotspot.setYaw(yaw);
            }
            if (pitch != null) {
                hotspot.setPitch(pitch);
            }
        }
        if (!inbound.isEmpty()) {
            hotspotRepository.saveAll(inbound);
            log.info("admin synced yaw/pitch for {} inbound hotspots -> scene {}", inbound.size(), sceneId);
        }
    }

    private static UUID parseScenePanoramaId(String unlockKey) {
        if (unlockKey == null || !unlockKey.startsWith("scene:")) {
            return null;
        }
        String raw = unlockKey.substring("scene:".length()).trim();
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public List<AdminArtifactResponse> listArtifacts(UUID locationId) {
        List<Artifact> items = locationId != null
                ? artifactRepository.findByLocationIdOrderBySortOrder(locationId)
                : artifactRepository.findByDeletedAtIsNull();
        return items.stream().map(AdminArtifactResponse::from).toList();
    }

    @Transactional
    public AdminArtifactResponse createArtifact(AdminArtifactRequest request) {
        Artifact saved = artifactRepository.saveAndFlush(Artifact.builder()
                .locationId(request.locationId())
                .name(request.name())
                .imageUrl(request.imageUrl())
                .description(request.description())
                .story(request.story())
                .unlockKey(request.unlockKey())
                .reliability(request.reliability())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .collectionName(request.collectionName())
                .rarity(request.rarity())
                .sourceUrl(request.sourceUrl())
                .imageSource(request.imageSource())
                .imageLicense(request.imageLicense())
                .factVerified(Boolean.TRUE.equals(request.factVerified()))
                .unlockMinigameId(request.unlockMinigameId())
                .build());
        audit.append(new AdminAuditCommand(actorId(), "ARTIFACT", saved.getId(), "CREATED", null,
                saved.getRowVersion(), null, null, Map.of("name", saved.getName())));
        log.info("admin create artifact id={} locationId={}", saved.getId(), saved.getLocationId());
        return AdminArtifactResponse.from(saved);
    }

    @Transactional
    public AdminArtifactResponse updateArtifact(UUID id, AdminArtifactRequest request) {
        Artifact artifact = artifactRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + id));
        requireVersion(artifact.getRowVersion(), request.rowVersion());
        Long oldVersion = artifact.getRowVersion();
        artifact.setLocationId(request.locationId());
        artifact.setName(request.name());
        artifact.setImageUrl(request.imageUrl());
        artifact.setDescription(request.description());
        artifact.setStory(request.story());
        artifact.setUnlockKey(request.unlockKey());
        artifact.setReliability(request.reliability());
        if (request.sortOrder() != null) {
            artifact.setSortOrder(request.sortOrder());
        }
        artifact.setCollectionName(request.collectionName());
        artifact.setRarity(request.rarity());
        artifact.setSourceUrl(request.sourceUrl());
        artifact.setImageSource(request.imageSource());
        artifact.setImageLicense(request.imageLicense());
        if (request.factVerified() != null) {
            artifact.setFactVerified(request.factVerified());
        }
        artifact.setUnlockMinigameId(request.unlockMinigameId());
        Artifact saved = artifactRepository.saveAndFlush(artifact);
        audit.append(new AdminAuditCommand(actorId(), "ARTIFACT", id, "UPDATED", oldVersion,
                saved.getRowVersion(), null, null, Map.of("name", saved.getName())));
        return AdminArtifactResponse.from(saved);
    }

    public List<AdminQuestResponse> listQuests(UUID locationId) {
        List<Quest> quests =
                locationId != null ? questRepository.findByLocationId(locationId) : questRepository.findByDeletedAtIsNull();
        return quests.stream().map(AdminQuestResponse::from).toList();
    }

    @Transactional
    public AdminQuestResponse createQuest(AdminQuestRequest request) {
        if (request.locationId() != null) {
            locationEraValidationService.ensureMinimumThreeEras(request.locationId());
        }
        Quest saved = questRepository.saveAndFlush(Quest.builder()
                .locationId(request.locationId())
                .title(request.title())
                .description(request.description())
                .story(request.story())
                .pointsReward(request.pointsReward() != null ? request.pointsReward() : 0)
                .requiredOrder(request.requiredOrder() != null ? request.requiredOrder() : 0)
                .completionTrigger(request.completionTrigger() != null ? request.completionTrigger() : "discovery")
                .requireOnsiteCheckin(Boolean.TRUE.equals(request.requireOnsiteCheckin()))
                .stepsTotal(request.stepsTotal())
                .coverImage(request.coverImage())
                .stepDiscoveryKeys(request.stepDiscoveryKeys())
                .build());
        audit.append(new AdminAuditCommand(actorId(), "QUEST", saved.getId(), "CREATED", null,
                saved.getRowVersion(), null, null, Map.of("title", saved.getTitle())));
        log.info("admin create quest id={} locationId={}", saved.getId(), saved.getLocationId());
        return AdminQuestResponse.from(saved);
    }

    @Transactional
    public AdminQuestResponse updateQuest(UUID id, AdminQuestRequest request) {
        Quest quest = questRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + id));
        requireVersion(quest.getRowVersion(), request.rowVersion());
        Long oldVersion = quest.getRowVersion();
        quest.setLocationId(request.locationId());
        quest.setTitle(request.title());
        quest.setDescription(request.description());
        quest.setStory(request.story());
        if (request.pointsReward() != null) {
            quest.setPointsReward(request.pointsReward());
        }
        if (request.requiredOrder() != null) {
            quest.setRequiredOrder(request.requiredOrder());
        }
        if (request.completionTrigger() != null) {
            quest.setCompletionTrigger(request.completionTrigger());
        }
        if (request.requireOnsiteCheckin() != null) {
            quest.setRequireOnsiteCheckin(request.requireOnsiteCheckin());
        }
        if (request.stepsTotal() != null) {
            quest.setStepsTotal(request.stepsTotal());
        }
        if (request.coverImage() != null) {
            quest.setCoverImage(request.coverImage());
        }
        if (request.stepDiscoveryKeys() != null) {
            quest.setStepDiscoveryKeys(request.stepDiscoveryKeys());
        }
        Quest saved = questRepository.saveAndFlush(quest);
        audit.append(new AdminAuditCommand(actorId(), "QUEST", id, "UPDATED", oldVersion,
                saved.getRowVersion(), null, null, Map.of("title", saved.getTitle())));
        return AdminQuestResponse.from(saved);
    }

    private UUID actorId() {
        return currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }

    private void requireVersion(Long currentVersion, Long requestedVersion) {
        if (requestedVersion == null || !Objects.equals(currentVersion, requestedVersion)) {
            throw new ConflictException("Nội dung đã bị thay đổi; hãy tải lại trước khi lưu");
        }
    }
}
