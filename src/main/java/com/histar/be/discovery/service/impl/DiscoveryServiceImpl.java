package com.histar.be.discovery.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.GamificationProperties;
import com.histar.be.discovery.bridge.DiscoveryArtifactBridge;
import com.histar.be.discovery.dto.DiscoveryPointResponse;
import com.histar.be.discovery.dto.DiscoverySummaryResponse;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.profile.service.ProfileAccessPolicy;
import com.histar.be.gamification.rules.entity.UnlockRule;
import com.histar.be.gamification.rules.repository.UnlockRuleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final List<String> LEGACY_CHECKIN_KEYS = List.of("era:2026", "photo:cua-ham", "photo:gieng");

    private final DiscoveryPointRepository discoveryPointRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final DiscoveryArtifactBridge discoveryArtifactBridge;
    private final UnlockRuleRepository unlockRuleRepository;
    private final GamificationProperties gamificationProperties;
    private final ProfileAccessPolicy profileAccessPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<DiscoveryPointResponse> findPointsByLocation(UUID locationId) {
        return discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId).stream()
                .map(DiscoveryPointResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiscoverySummaryResponse summary(UUID userId, UUID locationId) {
        List<DiscoveryPoint> points = discoveryPointRepository.findByLocationIdOrderBySortOrder(locationId);
        Set<String> pointKeys = new HashSet<>();
        for (DiscoveryPoint point : points) {
            pointKeys.add(point.getUnlockKey());
        }
        if (profileAccessPolicy.previewsAllGamificationContent(userId)) {
            List<String> allKeys = points.stream().map(DiscoveryPoint::getUnlockKey).toList();
            return new DiscoverySummaryResponse(allKeys.size(), points.size(), allKeys, System.currentTimeMillis());
        }
        List<String> userKeys = userDiscoveryRepository.findByUserIdAndLocationId(userId, locationId).stream()
                .map(UserDiscovery::getDiscoveryKey)
                .filter(pointKeys::contains)
                .toList();
        long version = userDiscoveryRepository.findByUserIdAndLocationId(userId, locationId).stream()
                .filter(ud -> pointKeys.contains(ud.getDiscoveryKey()))
                .mapToLong(ud -> ud.getDiscoveredAt() != null ? ud.getDiscoveredAt().toEpochMilli() : 0L)
                .max()
                .orElse(0L);
        return new DiscoverySummaryResponse(userKeys.size(), points.size(), userKeys, version);
    }

    @Override
    @Transactional
    public boolean record(UUID userId, String unlockKey) {
        return record(userId, unlockKey, null);
    }

    @Override
    @Transactional
    public boolean record(UUID userId, String unlockKey, UUID locationId) {
        DiscoveryPoint point = resolvePoint(unlockKey, locationId);
        UUID resolvedLocationId = point.getLocationId();
        if (userDiscoveryRepository.existsByUserIdAndLocationIdAndDiscoveryKey(
                userId, resolvedLocationId, unlockKey)) {
            return false;
        }
        userDiscoveryRepository.save(UserDiscovery.builder()
                .userId(userId)
                .locationId(resolvedLocationId)
                .discoveryKey(unlockKey)
                .discoveredAt(Instant.now())
                .build());
        return true;
    }

    private DiscoveryPoint resolvePoint(String unlockKey, UUID locationId) {
        if (locationId == null) {
            throw new BusinessRuleException("locationId bắt buộc khi ghi discovery: " + unlockKey);
        }
        return discoveryPointRepository
                .findByUnlockKeyAndLocationId(unlockKey, locationId)
                .orElseThrow(() -> new BusinessRuleException("unlock_key không hợp lệ tại địa điểm: " + unlockKey));
    }

    @Override
    @Transactional
    public void recordOnCheckin(UUID userId, UUID locationId) {
        List<String> keys = resolveCheckinDiscoveryKeys(locationId);
        for (String key : keys) {
            if (discoveryPointRepository.findByUnlockKeyAndLocationId(key, locationId).isEmpty()) {
                continue;
            }
            if (record(userId, key, locationId)) {
                discoveryArtifactBridge.unlockLinkedArtifacts(userId, key);
            } else {
                discoveryArtifactBridge.unlockLinkedArtifacts(userId, key);
            }
        }
    }

    private List<String> resolveCheckinDiscoveryKeys(UUID locationId) {
        if (gamificationProperties.isRulesEngineEnabled()) {
            List<String> fromRules = unlockRuleRepository
                    .findByLocationIdAndTriggerTypeAndEnabledTrueOrderBySortOrderAsc(locationId, "checkin")
                    .stream()
                    .filter(r -> "discovery".equals(r.getRewardType()))
                    .map(UnlockRule::getRewardKey)
                    .map(k -> resolveRewardKey(k, locationId))
                    .toList();
            if (!fromRules.isEmpty()) {
                return fromRules;
            }
        }
        List<String> keys = new ArrayList<>();
        for (String key : LEGACY_CHECKIN_KEYS) {
            if (discoveryPointRepository.findByUnlockKeyAndLocationId(key, locationId).isPresent()) {
                keys.add(key);
            }
        }
        return keys;
    }

    private String resolveRewardKey(String template, UUID locationId) {
        if (template != null && template.contains("{locationId}")) {
            return template.replace("{locationId}", locationId.toString());
        }
        return template;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPoints() {
        return discoveryPointRepository.count();
    }
}
