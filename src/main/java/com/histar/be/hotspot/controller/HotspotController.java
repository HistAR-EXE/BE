package com.histar.be.hotspot.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.hotspot.dto.HotspotResponse;
import com.histar.be.hotspot.service.HotspotService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotspots")
@RequiredArgsConstructor
public class HotspotController {

    private final HotspotService hotspotService;

    @GetMapping("/by-panorama/{panoramaId}")
    public ApiResponse<List<HotspotResponse>> findByPanorama(@PathVariable UUID panoramaId) {
        List<HotspotResponse> data = hotspotService.findByPanoramaId(panoramaId).stream()
                .map(HotspotResponse::from)
                .toList();
        return ApiResponse.ok(data);
    }
}
