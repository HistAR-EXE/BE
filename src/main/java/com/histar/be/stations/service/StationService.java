package com.histar.be.stations.service;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.stations.dto.StationBlockRequest;
import com.histar.be.stations.dto.StationBlockResponse;
import com.histar.be.stations.dto.StationBlockSnapshot;
import com.histar.be.stations.dto.StationContentSnapshot;
import com.histar.be.stations.dto.StationImportItem;
import com.histar.be.stations.dto.StationResponse;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.entity.StationContentBlock;
import com.histar.be.stations.repository.StationContentBlockRepository;
import com.histar.be.stations.repository.StationRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final StationContentBlockRepository blockRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AdminAuditService audit;

    @Transactional(readOnly = true)
    public List<StationResponse> listActive(String siteCode) {
        List<Station> stations = stationRepository.findBySiteCodeAndActiveTrueOrderBySortOrderAsc(siteCode);
        if (stations.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = stations.stream().map(Station::getId).toList();
        Map<UUID, List<StationBlockResponse>> byStation = new LinkedHashMap<>();
        for (StationContentBlock b : blockRepository.findByStationIdInOrderBySortOrderAsc(ids)) {
            byStation.computeIfAbsent(b.getStationId(), k -> new ArrayList<>()).add(toBlockResponse(b));
        }
        return stations.stream()
                .map(s -> toResponse(s, byStation.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public StationResponse getActive(String siteCode, String code) {
        Station station = stationRepository
                .findBySiteCodeAndCodeAndActiveTrue(siteCode, code)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + code));
        List<StationBlockResponse> blocks = blockRepository.findByStationIdOrderBySortOrderAsc(station.getId()).stream()
                .map(StationService::toBlockResponse)
                .toList();
        return toResponse(station, blocks);
    }

    /** Upserts stations by (siteCode, code); each station's blocks are replaced by the provided list. */
    @Transactional
    public List<StationResponse> importStations(String siteCode, List<StationImportItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleException("Import body must be a non-empty array");
        }
        Set<String> seen = new HashSet<>();
        for (StationImportItem item : items) {
            if (item.code() == null || item.code().isBlank() || item.name() == null || item.name().isBlank()) {
                throw new BusinessRuleException("Station code and name are required");
            }
            if (!seen.add(item.code())) {
                throw new BusinessRuleException("Duplicate station code in import: " + item.code());
            }
        }

        UUID actorId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));

        List<StationResponse> result = new ArrayList<>();
        int index = 0;
        for (StationImportItem item : items) {
            index++;
            Station station = stationRepository
                    .findBySiteCodeAndCode(siteCode, item.code())
                    .orElseGet(() -> Station.builder().siteCode(siteCode).code(item.code()).build());
            station.setName(item.name());
            station.setSortOrder(item.sortOrder() != null ? item.sortOrder() : index);
            station.setLat(item.lat());
            station.setLng(item.lng());
            station.setQuestStepKey(item.questStepKey());
            station.setActive(item.active() == null || item.active());
            station = stationRepository.saveAndFlush(station);

            blockRepository.deleteByStationId(station.getId());
            blockRepository.flush();

            List<StationContentBlock> saved = new ArrayList<>();
            List<StationBlockRequest> blocks = item.blocks() == null ? List.of() : item.blocks();
            int blockIndex = 0;
            for (StationBlockRequest b : blocks) {
                blockIndex++;
                saved.add(StationContentBlock.builder()
                        .stationId(station.getId())
                        .blockType(b.blockType())
                        .title(b.title())
                        .body(b.body())
                        .mediaUrl(b.mediaUrl())
                        .sortOrder(b.sortOrder() != null ? b.sortOrder() : blockIndex)
                        .metaJson(b.metaJson())
                        .build());
            }
            List<StationBlockResponse> blockResponses = blockRepository.saveAll(saved).stream()
                    .map(StationService::toBlockResponse)
                    .toList();
            result.add(toResponse(station, blockResponses));
        }
        List<UUID> stationIds = result.stream().map(StationResponse::id).toList();
        audit.append(new AdminAuditCommand(
                actorId, "STATION_IMPORT", stationIds.get(0), "STATIONS_IMPORTED",
                null, null, null, null,
                Map.of("siteCode", siteCode, "count", stationIds.size(), "stationIds", stationIds)));
        return result;
    }

    /**
     * Replaces one station's public fields and blocks from a reviewed snapshot.
     * Callers own the transaction and audit; this path does not grant admin import.
     */
    @Transactional
    public void applyPublishedSnapshot(String siteCode, StationContentSnapshot snapshot) {
        if (snapshot == null
                || snapshot.stationCode() == null
                || snapshot.stationCode().isBlank()
                || snapshot.name() == null
                || snapshot.name().isBlank()) {
            throw new BusinessRuleException("Station code and name are required");
        }
        String code = snapshot.stationCode().trim();
        Station station = stationRepository
                .findBySiteCodeAndCode(siteCode, code)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + code));
        station.setName(snapshot.name().trim());
        station.setSortOrder(snapshot.sortOrder());
        station.setLat(snapshot.lat());
        station.setLng(snapshot.lng());
        station.setQuestStepKey(snapshot.questStepKey());
        station.setActive(snapshot.active());
        station = stationRepository.saveAndFlush(station);

        blockRepository.deleteByStationId(station.getId());
        blockRepository.flush();

        List<StationContentBlock> saved = new ArrayList<>();
        List<StationBlockSnapshot> blocks = snapshot.blocks() == null ? List.of() : snapshot.blocks();
        for (StationBlockSnapshot block : blocks) {
            if (block.blockType() == null) {
                throw new BusinessRuleException("Loại block không hợp lệ");
            }
            saved.add(StationContentBlock.builder()
                    .stationId(station.getId())
                    .blockType(block.blockType())
                    .title(block.title())
                    .body(block.body())
                    .mediaUrl(block.mediaUrl())
                    .sortOrder(block.sortOrder())
                    .metaJson(block.metaJson())
                    .build());
        }
        if (!saved.isEmpty()) {
            blockRepository.saveAll(saved);
        }
    }

    private static StationResponse toResponse(Station s, List<StationBlockResponse> blocks) {
        return new StationResponse(
                s.getId(),
                s.getSiteCode(),
                s.getCode(),
                s.getName(),
                s.getSortOrder(),
                s.getLat(),
                s.getLng(),
                s.getQuestStepKey(),
                s.isActive(),
                blocks);
    }

    private static StationBlockResponse toBlockResponse(StationContentBlock b) {
        return new StationBlockResponse(
                b.getId(),
                b.getBlockType(),
                b.getTitle(),
                b.getBody(),
                b.getMediaUrl(),
                b.getSortOrder(),
                b.getMetaJson());
    }
}
