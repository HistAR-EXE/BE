package com.histar.be.checkin.service;

import com.histar.be.checkin.dto.CheckinHistoryItem;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.repository.StationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckinHistoryService {

    private final CheckinRepository checkinRepository;
    private final LocationRepository locationRepository;
    private final StationRepository stationRepository;

    @Transactional(readOnly = true)
    public List<CheckinHistoryItem> history(UUID userId, UUID locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
        String siteCode = location.getSiteCode() == null ? "" : location.getSiteCode().trim().toLowerCase(Locale.ROOT);
        Map<String, Station> stations = stationRepository.findBySiteCodeOrderBySortOrderAsc(siteCode).stream()
                .collect(Collectors.toMap(
                        station -> normalize(station.getCode()),
                        Function.identity(),
                        (first, ignored) -> first));
        Map<String, Checkin> latestByStation = new LinkedHashMap<>();
        for (Checkin checkin : checkinRepository.findByUserIdAndLocationIdOrderByCreatedAtDesc(userId, locationId)) {
            String code = normalize(checkin.getStationCode());
            if (code != null) latestByStation.putIfAbsent(code, checkin);
        }
        return latestByStation.entrySet().stream()
                .map(entry -> {
                    Checkin checkin = entry.getValue();
                    Station station = stations.get(entry.getKey());
                    return new CheckinHistoryItem(
                            checkin.getId(),
                            locationId,
                            siteCode,
                            entry.getKey(),
                            station == null ? entry.getKey() : station.getName(),
                            checkin.getCreatedAt(),
                            checkin.getPresenceMethod());
                })
                .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
