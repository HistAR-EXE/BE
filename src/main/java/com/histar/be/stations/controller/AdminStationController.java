package com.histar.be.stations.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.stations.dto.StationImportItem;
import com.histar.be.stations.dto.StationResponse;
import com.histar.be.stations.service.StationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sites/{siteCode}/stations")
@RequiredArgsConstructor
public class AdminStationController {

    private final StationService stationService;

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StationResponse>> importStations(
            @PathVariable String siteCode, @RequestBody @Valid List<StationImportItem> body) {
        return ApiResponse.ok(stationService.importStations(siteCode, body));
    }
}
