package com.histar.be.chat.service;

import com.histar.be.chat.dto.StationChatContext;
import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.entity.StationBlockType;
import com.histar.be.stations.repository.StationContentBlockRepository;
import com.histar.be.stations.repository.StationRepository;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StationChatContextResolver {

    private static final int MAX_SUMMARY_LENGTH = 1200;
    private final StationRepository stationRepository;
    private final StationContentBlockRepository blockRepository;

    @Transactional(readOnly = true)
    public Optional<StationChatContext> resolve(String siteCode, String stationCode) {
        if (stationCode == null || stationCode.isBlank()) return Optional.empty();
        String site = siteCode == null ? "" : siteCode.trim().toLowerCase(Locale.ROOT);
        String code = stationCode.trim().toUpperCase(Locale.ROOT);
        Station station = stationRepository.findBySiteCodeAndCodeAndActiveTrue(site, code)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.INVALID_STATION_CONTEXT,
                        "Trạm không tồn tại hoặc chưa hoạt động: " + site + "/" + code));
        String summary = blockRepository.findByStationIdOrderBySortOrderAsc(station.getId()).stream()
                .filter(block -> block.getBlockType() == StationBlockType.TEXT)
                .map(block -> block.getBody() == null ? "" : block.getBody().trim())
                .filter(body -> !body.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .map(value -> value.length() > MAX_SUMMARY_LENGTH ? value.substring(0, MAX_SUMMARY_LENGTH) : value)
                .orElse("");
        return Optional.of(new StationChatContext(site, code, station.getName(), summary));
    }
}
