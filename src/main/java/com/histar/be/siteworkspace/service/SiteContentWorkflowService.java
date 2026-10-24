package com.histar.be.siteworkspace.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.checkin.presence.StationQrService;
import com.histar.be.checkin.presence.StationQrService.SignedStationQr;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.siteworkspace.dto.ContentReviewDecision;
import com.histar.be.siteworkspace.dto.SiteContentDraftRequest;
import com.histar.be.siteworkspace.dto.SiteContentVersionResponse;
import com.histar.be.siteworkspace.dto.SiteProfileSnapshot;
import com.histar.be.siteworkspace.dto.SiteStationQrResponse;
import com.histar.be.siteworkspace.entity.ContentLifecycleStatus;
import com.histar.be.siteworkspace.entity.SiteContentKind;
import com.histar.be.siteworkspace.entity.SiteContentVersion;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.repository.SiteContentVersionRepository;
import com.histar.be.stations.dto.StationBlockSnapshot;
import com.histar.be.stations.dto.StationContentSnapshot;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.entity.StationBlockType;
import com.histar.be.stations.repository.StationRepository;
import com.histar.be.stations.service.StationService;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteContentWorkflowService {

    /** Public path key. The row stores {@code locations.site_code} to satisfy the profile-key check. */
    static final String PROFILE_RESOURCE_KEY = "profile";

    private static final String STATION_NOT_FOUND = "Không tìm thấy trạm";
    private static final String VERSION_NOT_FOUND = "Không tìm thấy phiên bản nội dung";
    private static final String STALE_VERSION = "Content version was changed by another editor";
    private static final Pattern MEDIA_URL = Pattern.compile("^(https://|http://|/)\\S+$");
    private static final Set<SiteRole> EDITORS = Set.of(SiteRole.SITE_MANAGER, SiteRole.CONTENT_EDITOR);
    private static final Set<SiteRole> REVIEWERS = Set.of(SiteRole.CONTENT_REVIEWER, SiteRole.SITE_MANAGER);
    private static final Set<SiteRole> PUBLISHERS = Set.of(SiteRole.SITE_MANAGER);
    private static final List<ContentLifecycleStatus> IN_FLIGHT = List.of(
            ContentLifecycleStatus.DRAFT, ContentLifecycleStatus.IN_REVIEW, ContentLifecycleStatus.APPROVED);

    private final SiteContentVersionRepository versions;
    private final LocationRepository locations;
    private final StationRepository stationRepository;
    private final StationService stationService;
    private final SiteWorkspaceAccessService access;
    private final AdminAuditService audit;
    private final StationQrService stationQrService;
    private final ObjectMapper objectMapper;

    public SiteContentWorkflowService(
            SiteContentVersionRepository versions,
            LocationRepository locations,
            StationRepository stationRepository,
            StationService stationService,
            SiteWorkspaceAccessService access,
            AdminAuditService audit,
            StationQrService stationQrService,
            ObjectMapper objectMapper) {
        this.versions = versions;
        this.locations = locations;
        this.stationRepository = stationRepository;
        this.stationService = stationService;
        this.access = access;
        this.audit = audit;
        this.stationQrService = stationQrService;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    @Transactional
    public SiteContentVersionResponse createDraft(
            UUID userId, String siteCode, SiteContentKind contentKind, String resourceKey, SiteContentDraftRequest request) {
        Location location = authorize(userId, siteCode, EDITORS);
        ResolvedResource resource = resolve(location, contentKind, resourceKey, true);
        if (inFlight(location.getSiteCode(), contentKind, resource.persistedKey())) {
            throw new BusinessRuleException("Đã có phiên bản nội dung đang xử lý");
        }
        String snapshotJson = writeSnapshot(contentKind, request == null ? null : request.snapshot(), resource, false);
        int nextVersion = versions
                .findFirstBySiteCodeIgnoreCaseAndContentKindAndResourceKeyOrderByVersionDesc(
                        location.getSiteCode(), contentKind, resource.persistedKey())
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);
        SiteContentVersion created = SiteContentVersion.builder()
                .siteCode(location.getSiteCode())
                .contentKind(contentKind)
                .resourceKey(resource.persistedKey())
                .locationId(resource.locationId())
                .stationId(resource.stationId())
                .version(nextVersion)
                .status(ContentLifecycleStatus.DRAFT)
                .snapshotJson(snapshotJson)
                .createdBy(userId)
                .build();
        return toResponse(save(created));
    }

    @Transactional
    public SiteContentVersionResponse updateDraft(
            UUID userId,
            String siteCode,
            SiteContentKind contentKind,
            String resourceKey,
            int version,
            long expectedRevision,
            SiteContentDraftRequest request) {
        Location location = authorize(userId, siteCode, EDITORS);
        ResolvedResource resource = resolve(location, contentKind, resourceKey, false);
        SiteContentVersion current = load(location.getSiteCode(), contentKind, resource.persistedKey(), version);
        requireRevision(current, expectedRevision);
        if (current.getStatus() != ContentLifecycleStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ bản nháp mới được sửa");
        }
        current.setSnapshotJson(writeSnapshot(contentKind, request == null ? null : request.snapshot(), resource, false));
        current.setUpdatedAt(Instant.now());
        return toResponse(save(current));
    }

    @Transactional
    public SiteContentVersionResponse submitForReview(
            UUID userId,
            String siteCode,
            SiteContentKind contentKind,
            String resourceKey,
            int version,
            long expectedRevision) {
        Location location = authorize(userId, siteCode, EDITORS);
        String key = persistedKey(location, contentKind, resourceKey);
        SiteContentVersion current = load(location.getSiteCode(), contentKind, key, version);
        requireRevision(current, expectedRevision);
        if (current.getStatus() != ContentLifecycleStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ bản nháp mới được gửi duyệt");
        }
        long previousRevision = current.getRevision();
        current.setStatus(ContentLifecycleStatus.IN_REVIEW);
        current.setSubmittedBy(userId);
        current.setSubmittedAt(Instant.now());
        current.setReviewedBy(null);
        current.setReviewedAt(null);
        current.setReviewNote(null);
        current.setUpdatedAt(Instant.now());
        SiteContentVersion saved = save(current);
        audit(userId, saved, "SITE_CONTENT_SUBMITTED", previousRevision, null, null);
        return toResponse(saved);
    }

    @Transactional
    public SiteContentVersionResponse review(
            UUID userId,
            String siteCode,
            SiteContentKind contentKind,
            String resourceKey,
            int version,
            long expectedRevision,
            ContentReviewDecision decision,
            String note) {
        Location location = authorize(userId, siteCode, REVIEWERS);
        String key = persistedKey(location, contentKind, resourceKey);
        SiteContentVersion current = load(location.getSiteCode(), contentKind, key, version);
        requireRevision(current, expectedRevision);
        if (current.getStatus() != ContentLifecycleStatus.IN_REVIEW) {
            throw new BusinessRuleException("Chỉ bản đang chờ duyệt mới được xét");
        }
        if (decision == null) {
            throw new BusinessRuleException("Quyết định duyệt không hợp lệ");
        }
        if (current.getSubmittedBy() == null || userId.equals(current.getSubmittedBy())) {
            throw new BusinessRuleException("Không thể tự duyệt nội dung do chính mình gửi");
        }
        if (decision == ContentReviewDecision.APPROVE) {
            readReadySnapshot(contentKind, current.getSnapshotJson(), key);
        }
        long previousRevision = current.getRevision();
        current.setStatus(decision == ContentReviewDecision.APPROVE
                ? ContentLifecycleStatus.APPROVED
                : ContentLifecycleStatus.DRAFT);
        current.setReviewedBy(userId);
        current.setReviewedAt(Instant.now());
        current.setReviewNote(blankToNull(note));
        current.setUpdatedAt(Instant.now());
        SiteContentVersion saved = save(current);
        audit(userId, saved, "SITE_CONTENT_REVIEWED", previousRevision, blankToNull(note), decision);
        return toResponse(saved);
    }

    @Transactional
    public SiteContentVersionResponse publish(
            UUID userId,
            String siteCode,
            SiteContentKind contentKind,
            String resourceKey,
            int version,
            long expectedRevision) {
        Location location = authorize(userId, siteCode, PUBLISHERS);
        String key = persistedKey(location, contentKind, resourceKey);
        SiteContentVersion current = load(location.getSiteCode(), contentKind, key, version);
        requireRevision(current, expectedRevision);
        if (current.getStatus() != ContentLifecycleStatus.APPROVED) {
            throw new BusinessRuleException("Chỉ bản đã duyệt mới được phát hành");
        }
        applySnapshot(location, contentKind, current.getSnapshotJson(), key);
        archivePublished(location.getSiteCode(), contentKind, key, current.getId());
        long previousRevision = current.getRevision();
        current.setStatus(ContentLifecycleStatus.PUBLISHED);
        current.setPublishedBy(userId);
        current.setPublishedAt(Instant.now());
        current.setUpdatedAt(Instant.now());
        SiteContentVersion saved = save(current);
        audit(userId, saved, "SITE_CONTENT_PUBLISHED", previousRevision, null, null);
        return toResponse(saved);
    }

    @Transactional
    public SiteContentVersionResponse rollback(
            UUID userId,
            String siteCode,
            SiteContentKind contentKind,
            String resourceKey,
            int publishedVersion,
            String reason,
            long expectedRevision) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Lý do thu hồi là bắt buộc");
        }
        Location location = authorize(userId, siteCode, PUBLISHERS);
        String key = persistedKey(location, contentKind, resourceKey);
        SiteContentVersion target = load(location.getSiteCode(), contentKind, key, publishedVersion);
        if (target.getStatus() != ContentLifecycleStatus.ARCHIVED || target.getPublishedAt() == null) {
            throw new BusinessRuleException("Chỉ có thể khôi phục một bản đã phát hành");
        }
        List<SiteContentVersion> live = versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                location.getSiteCode(), contentKind, key, ContentLifecycleStatus.PUBLISHED);
        if (live == null || live.size() != 1) {
            throw new BusinessRuleException("Không có đúng một bản đang phát hành để thu hồi");
        }
        SiteContentVersion current = live.get(0);
        requireRevision(current, expectedRevision);
        String restoredJson = target.getSnapshotJson();
        applySnapshot(location, contentKind, restoredJson, key);
        current.setStatus(ContentLifecycleStatus.ARCHIVED);
        current.setUpdatedAt(Instant.now());
        save(current);
        int nextVersion = versions
                .findFirstBySiteCodeIgnoreCaseAndContentKindAndResourceKeyOrderByVersionDesc(
                        location.getSiteCode(), contentKind, key)
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);
        SiteContentVersion restored = SiteContentVersion.builder()
                .siteCode(location.getSiteCode())
                .contentKind(contentKind)
                .resourceKey(key)
                .locationId(contentKind == SiteContentKind.SITE_PROFILE ? location.getId() : null)
                .stationId(contentKind == SiteContentKind.STATION ? target.getStationId() : null)
                .version(nextVersion)
                .status(ContentLifecycleStatus.PUBLISHED)
                .snapshotJson(restoredJson)
                .createdBy(target.getCreatedBy() == null ? userId : target.getCreatedBy())
                .submittedBy(target.getSubmittedBy())
                .reviewedBy(target.getReviewedBy())
                .publishedBy(userId)
                .reviewNote(reason.trim())
                .submittedAt(target.getSubmittedAt())
                .reviewedAt(target.getReviewedAt())
                .publishedAt(Instant.now())
                .build();
        SiteContentVersion saved = save(restored);
        audit(userId, saved, "SITE_CONTENT_ROLLED_BACK", current.getRevision(), reason.trim(), null);
        return toResponse(saved);
    }

    public SiteStationQrResponse issueStaticQr(UUID userId, String siteCode, String stationCode) {
        Location location = authorize(userId, siteCode, EDITORS);
        Station station = requireStation(location.getSiteCode(), stationCode);
        if (!station.isActive()) {
            throw new ResourceNotFoundException(STATION_NOT_FOUND);
        }
        SignedStationQr qr = stationQrService.issueStatic(location.getSiteCode(), station.getCode());
        return new SiteStationQrResponse(
                qr.siteCode(), qr.stationCode(), qr.timestamp(), qr.signature(), qr.payload(), qr.signed(), qr.staticQr());
    }

    public void applyPublishedSiteProfile(Location location, SiteProfileSnapshot snapshot) {
        location.setName(snapshot.name());
        location.setDescription(snapshot.description());
        location.setFormattedAddress(snapshot.formattedAddress());
        location.setCoverImage(snapshot.coverImage());
        location.setSources(snapshot.sources());
        locations.save(location);
    }

    private void applySnapshot(Location location, SiteContentKind kind, String snapshotJson, String persistedKey) {
        if (kind == SiteContentKind.SITE_PROFILE) {
            applyPublishedSiteProfile(location, readProfile(readTree(snapshotJson), true));
            return;
        }
        stationService.applyPublishedSnapshot(location.getSiteCode(), readStation(readTree(snapshotJson), persistedKey, true));
    }

    private Object readReadySnapshot(SiteContentKind kind, String snapshotJson, String persistedKey) {
        JsonNode tree = readTree(snapshotJson);
        if (kind == SiteContentKind.SITE_PROFILE) {
            return readProfile(tree, true);
        }
        return readStation(tree, persistedKey, true);
    }

    private String writeSnapshot(SiteContentKind kind, JsonNode snapshot, ResolvedResource resource, boolean strict) {
        try {
            if (kind == SiteContentKind.SITE_PROFILE) {
                return objectMapper.writeValueAsString(readProfile(snapshot, strict));
            }
            return objectMapper.writeValueAsString(readStation(snapshot, resource.persistedKey(), strict));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot write content snapshot", ex);
        }
    }

    private SiteProfileSnapshot readProfile(JsonNode snapshot, boolean strict) {
        if (snapshot == null || !snapshot.isObject()) {
            throw new BusinessRuleException("Snapshot hồ sơ site không hợp lệ");
        }
        SiteProfileSnapshot parsed;
        try {
            parsed = objectMapper.treeToValue(snapshot, SiteProfileSnapshot.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessRuleException("Snapshot hồ sơ site chứa trường không được phép");
        }
        if (strict && (parsed.name() == null || parsed.name().isBlank())) {
            throw new BusinessRuleException("Tên hồ sơ site là bắt buộc");
        }
        return parsed;
    }

    private StationContentSnapshot readStation(JsonNode snapshot, String stationCode, boolean strict) {
        if (snapshot == null || !snapshot.isObject()) {
            throw new BusinessRuleException("Snapshot trạm không hợp lệ");
        }
        StationContentSnapshot parsed;
        try {
            parsed = objectMapper.treeToValue(snapshot, StationContentSnapshot.class);
        } catch (JsonProcessingException ex) {
            if (invalidBlockType(ex)) {
                throw new BusinessRuleException("Loại block không hợp lệ");
            }
            throw new BusinessRuleException("Snapshot trạm không hợp lệ");
        }
        if (parsed.stationCode() == null || !parsed.stationCode().equalsIgnoreCase(stationCode)) {
            throw new BusinessRuleException("stationCode không khớp resource key");
        }
        if (strict && (parsed.name() == null || parsed.name().isBlank())) {
            throw new BusinessRuleException("Tên trạm là bắt buộc");
        }
        List<StationBlockSnapshot> blocks = new ArrayList<>();
        if (parsed.blocks() != null) {
            for (StationBlockSnapshot block : parsed.blocks()) {
                blocks.add(validateBlock(block, strict));
            }
        }
        return new StationContentSnapshot(
                stationCode,
                parsed.name() == null ? null : parsed.name().trim(),
                parsed.sortOrder(),
                parsed.lat(),
                parsed.lng(),
                parsed.questStepKey(),
                parsed.active(),
                blocks);
    }

    private StationBlockSnapshot validateBlock(StationBlockSnapshot block, boolean strict) {
        if (block == null || block.blockType() == null) {
            throw new BusinessRuleException("Loại block không hợp lệ");
        }
        String media = block.mediaUrl() == null ? null : block.mediaUrl().trim();
        boolean blank = media == null || media.isBlank();
        boolean playback = block.blockType() == StationBlockType.IMAGE
                || block.blockType() == StationBlockType.AUDIO
                || block.blockType() == StationBlockType.VIDEO;
        if (playback && blank && strict) {
            throw new BusinessRuleException("Block " + block.blockType() + " cần mediaUrl");
        }
        if (!blank && !MEDIA_URL.matcher(media).matches()) {
            throw new BusinessRuleException("mediaUrl không hợp lệ");
        }
        return new StationBlockSnapshot(
                block.blockType(), block.title(), block.body(), blank ? null : media, block.sortOrder(), block.metaJson());
    }

    private boolean invalidBlockType(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof InvalidFormatException invalid && invalid.getTargetType() == StationBlockType.class) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void archivePublished(String siteCode, SiteContentKind kind, String key, UUID keepId) {
        List<SiteContentVersion> live = versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                siteCode, kind, key, ContentLifecycleStatus.PUBLISHED);
        if (live == null) {
            return;
        }
        for (SiteContentVersion version : live) {
            if (keepId != null && keepId.equals(version.getId())) {
                continue;
            }
            version.setStatus(ContentLifecycleStatus.ARCHIVED);
            version.setUpdatedAt(Instant.now());
            save(version);
        }
    }

    private boolean inFlight(String siteCode, SiteContentKind kind, String key) {
        for (ContentLifecycleStatus status : IN_FLIGHT) {
            List<SiteContentVersion> rows = versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                    siteCode, kind, key, status);
            if (rows != null && !rows.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Location authorize(UUID userId, String siteCode, Set<SiteRole> roles) {
        Location location = resolveSite(siteCode);
        access.requireAny(userId, location.getSiteCode(), roles);
        return location;
    }

    private Location resolveSite(String siteCode) {
        if (siteCode == null || siteCode.isBlank()) {
            throw new BusinessRuleException("Không tìm thấy site workspace");
        }
        return locations.findBySiteCodeIgnoreCase(siteCode.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy site workspace"));
    }

    private ResolvedResource resolve(Location location, SiteContentKind kind, String resourceKey, boolean bindStation) {
        String key = persistedKey(location, kind, resourceKey);
        if (kind == SiteContentKind.SITE_PROFILE) {
            return new ResolvedResource(key, location.getId(), null);
        }
        if (!bindStation) {
            return new ResolvedResource(key, null, null);
        }
        Station station = requireStation(location.getSiteCode(), key);
        return new ResolvedResource(station.getCode(), null, station.getId());
    }

    private String persistedKey(Location location, SiteContentKind kind, String resourceKey) {
        if (kind == SiteContentKind.SITE_PROFILE) {
            if (resourceKey == null || !PROFILE_RESOURCE_KEY.equals(resourceKey.trim().toLowerCase(Locale.ROOT))) {
                throw new BusinessRuleException("SITE_PROFILE dùng resource key profile");
            }
            return location.getSiteCode();
        }
        if (kind == SiteContentKind.STATION) {
            return StationQrService.normalizeStationCode(resourceKey);
        }
        throw new BusinessRuleException("contentKind không hợp lệ");
    }

    private Station requireStation(String siteCode, String stationCode) {
        String raw = stationCode == null ? "" : stationCode.trim();
        Optional<Station> found = raw.isEmpty()
                ? Optional.empty()
                : stationRepository.findBySiteCodeAndCode(siteCode, raw);
        if (found.isEmpty() && !raw.isEmpty()) {
            try {
                String normalized = StationQrService.normalizeStationCode(raw);
                if (!normalized.equals(raw)) {
                    found = stationRepository.findBySiteCodeAndCode(siteCode, normalized);
                }
            } catch (BusinessRuleException ignored) {
                found = Optional.empty();
            }
        }
        return found.orElseThrow(() -> new ResourceNotFoundException(STATION_NOT_FOUND));
    }

    private SiteContentVersion load(String siteCode, SiteContentKind kind, String key, int version) {
        return versions
                .findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndVersion(siteCode, kind, key, version)
                .orElseThrow(() -> new ResourceNotFoundException(VERSION_NOT_FOUND));
    }

    private void requireRevision(SiteContentVersion version, long expectedRevision) {
        if (version.getRevision() != expectedRevision) {
            throw new NarrativeConflictException(ErrorCode.VERSION_CONFLICT, STALE_VERSION);
        }
    }

    private SiteContentVersion save(SiteContentVersion version) {
        try {
            return versions.saveAndFlush(version);
        } catch (OptimisticLockingFailureException | OptimisticLockException ex) {
            throw new NarrativeConflictException(ErrorCode.VERSION_CONFLICT, STALE_VERSION);
        }
    }

    private void audit(
            UUID actorId,
            SiteContentVersion version,
            String action,
            long previousRevision,
            String reason,
            ContentReviewDecision decision) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("siteCode", version.getSiteCode());
        metadata.put("contentKind", version.getContentKind().name());
        metadata.put("resourceKey", apiResourceKey(version));
        metadata.put("version", version.getVersion());
        metadata.put("status", version.getStatus().name());
        if (decision != null) {
            metadata.put("decision", decision.name());
        }
        audit.append(new AdminAuditCommand(
                actorId,
                "SITE_CONTENT",
                version.getId(),
                action,
                previousRevision,
                version.getRevision(),
                reason,
                "site-content:" + action + ":" + version.getId() + ":" + version.getRevision() + ":" + version.getStatus(),
                metadata));
    }

    private SiteContentVersionResponse toResponse(SiteContentVersion version) {
        return new SiteContentVersionResponse(
                version.getId(),
                version.getSiteCode(),
                version.getContentKind(),
                apiResourceKey(version),
                version.getLocationId(),
                version.getStationId(),
                version.getVersion(),
                version.getStatus(),
                version.getSnapshotJson(),
                version.getRevision(),
                version.getCreatedBy(),
                version.getSubmittedBy(),
                version.getReviewedBy(),
                version.getPublishedBy(),
                version.getReviewNote(),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                version.getSubmittedAt(),
                version.getReviewedAt(),
                version.getPublishedAt());
    }

    private String apiResourceKey(SiteContentVersion version) {
        return version.getContentKind() == SiteContentKind.SITE_PROFILE
                ? PROFILE_RESOURCE_KEY
                : version.getResourceKey();
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new BusinessRuleException("Snapshot nội dung không hợp lệ");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ResolvedResource(String persistedKey, UUID locationId, UUID stationId) {}
}
