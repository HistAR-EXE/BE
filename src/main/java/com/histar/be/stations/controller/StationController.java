package com.histar.be.stations.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.stations.dto.StationResponse;
import com.histar.be.stations.service.StationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteCode}/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping
    public ApiResponse<List<StationResponse>> list(@PathVariable String siteCode) {
        return ApiResponse.ok(stationService.listActive(siteCode));
    }

    @GetMapping("/{code}")
    public ApiResponse<StationResponse> get(@PathVariable String siteCode, @PathVariable String code) {
        return ApiResponse.ok(stationService.getActive(siteCode, code));
    }
}
