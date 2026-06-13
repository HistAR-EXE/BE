package com.histar.be.admin.service;

import com.histar.be.admin.dto.AdminArtifactRequest;
import com.histar.be.admin.dto.AdminArtifactResponse;
import com.histar.be.admin.dto.AdminDiscoveryPointRequest;
import com.histar.be.admin.dto.AdminDiscoveryPointResponse;
import com.histar.be.admin.dto.AdminQuestRequest;
import com.histar.be.admin.dto.AdminQuestResponse;
import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import java.util.List;
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

    public List<AdminDiscoveryPointResponse> listDiscoveryPoints(UUID locationId) {
        List<DiscoveryPoint> points = locationId != null
                ? discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId)
                : discoveryPointRepository.findAll();
        return points.stream().map(AdminDiscoveryPointResponse::from).toList();
    }

    @Transactional
    public AdminDiscoveryPointResponse createDiscoveryPoint(AdminDiscoveryPointRequest request) {
        DiscoveryPoint saved = discoveryPointRepository.save(DiscoveryPoint.builder()
                .locationId(request.locationId())
                .name(request.name())
                .mapXPct(request.mapXPct())
                .mapYPct(request.mapYPct())
                .unlockKey(request.unlockKey())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build());
        log.info("admin create discovery-point id={} locationId={}", saved.getId(), saved.getLocationId());
        return AdminDiscoveryPointResponse.from(saved);
    }

    @Transactional
    public AdminDiscoveryPointResponse updateDiscoveryPoint(UUID id, AdminDiscoveryPointRequest request) {
        DiscoveryPoint point = discoveryPointRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discovery point not found: " + id));
        point.setLocationId(request.locationId());
        point.setName(request.name());
        point.setMapXPct(request.mapXPct());
        point.setMapYPct(request.mapYPct());
        point.setUnlockKey(request.unlockKey());
        if (request.sortOrder() != null) {
            point.setSortOrder(request.sortOrder());
        }
        return AdminDiscoveryPointResponse.from(discoveryPointRepository.save(point));
    }

    public List<AdminArtifactResponse> listArtifacts(UUID locationId) {
        List<Artifact> items = locationId != null
                ? artifactRepository.findByLocationIdOrderBySortOrder(locationId)
                : artifactRepository.findAll();
        return items.stream().map(AdminArtifactResponse::from).toList();
    }

    @Transactional
    public AdminArtifactResponse createArtifact(AdminArtifactRequest request) {
        Artifact saved = artifactRepository.save(Artifact.builder()
                .locationId(request.locationId())
                .name(request.name())
                .imageUrl(request.imageUrl())
                .description(request.description())
                .unlockKey(request.unlockKey())
                .reliability(request.reliability())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build());
        log.info("admin create artifact id={} locationId={}", saved.getId(), saved.getLocationId());
        return AdminArtifactResponse.from(saved);
    }

    @Transactional
    public AdminArtifactResponse updateArtifact(UUID id, AdminArtifactRequest request) {
        Artifact artifact = artifactRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + id));
        artifact.setLocationId(request.locationId());
        artifact.setName(request.name());
        artifact.setImageUrl(request.imageUrl());
        artifact.setDescription(request.description());
        artifact.setUnlockKey(request.unlockKey());
        artifact.setReliability(request.reliability());
        if (request.sortOrder() != null) {
            artifact.setSortOrder(request.sortOrder());
        }
        return AdminArtifactResponse.from(artifactRepository.save(artifact));
    }

    public List<AdminQuestResponse> listQuests(UUID locationId) {
        List<Quest> quests =
                locationId != null ? questRepository.findByLocationId(locationId) : questRepository.findAll();
        return quests.stream().map(AdminQuestResponse::from).toList();
    }

    @Transactional
    public AdminQuestResponse createQuest(AdminQuestRequest request) {
        Quest saved = questRepository.save(Quest.builder()
                .locationId(request.locationId())
                .title(request.title())
                .description(request.description())
                .story(request.story())
                .pointsReward(request.pointsReward() != null ? request.pointsReward() : 0)
                .requiredOrder(request.requiredOrder() != null ? request.requiredOrder() : 0)
                .build());
        log.info("admin create quest id={} locationId={}", saved.getId(), saved.getLocationId());
        return AdminQuestResponse.from(saved);
    }

    @Transactional
    public AdminQuestResponse updateQuest(UUID id, AdminQuestRequest request) {
        Quest quest = questRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + id));
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
        return AdminQuestResponse.from(questRepository.save(quest));
    }
}
