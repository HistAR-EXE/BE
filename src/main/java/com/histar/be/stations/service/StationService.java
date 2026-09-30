package com.histar.be.stations.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.stations.dto.StationBlockRequest;
import com.histar.be.stations.dto.StationBlockResponse;
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
        return result;
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
