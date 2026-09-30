package com.histar.be.pilot.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.pilot.dto.PilotSiteResponse;
import com.histar.be.stations.repository.StationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pilot-sites")
@RequiredArgsConstructor
public class PilotSitesController {

    private static final Map<String, String> REGION_BY_SITE = new LinkedHashMap<>();

    static {
        REGION_BY_SITE.put("cu-chi", "Nam");
        REGION_BY_SITE.put("hoang-thanh-thang-long", "Bắc");
        REGION_BY_SITE.put("dai-noi-hue", "Trung");
    }

    private final LocationRepository locationRepository;
    private final StationRepository stationRepository;

    @GetMapping
    public ApiResponse<List<PilotSiteResponse>> list() {
        List<PilotSiteResponse> sites = locationRepository.findBySiteCodeIsNotNullOrderByNameAsc().stream()
                .filter(loc -> loc.getSiteCode() != null && REGION_BY_SITE.containsKey(loc.getSiteCode().toLowerCase(Locale.ROOT)))
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(sites);
    }

    private PilotSiteResponse toResponse(Location loc) {
        String site = loc.getSiteCode().trim().toLowerCase(Locale.ROOT);
        int stationCount = stationRepository.findBySiteCodeAndActiveTrueOrderBySortOrderAsc(site).size();
        return new PilotSiteResponse(
                site,
                loc.getId(),
                loc.getName(),
                REGION_BY_SITE.getOrDefault(site, "Khác"),
                loc.getFormattedAddress(),
                loc.getLatitude(),
                loc.getLongitude(),
                stationCount >= 6);
    }
}
