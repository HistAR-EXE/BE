package com.histar.be.panorama.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.panorama.dto.PanoramaResponse;
import com.histar.be.panorama.service.PanoramaService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panoramas")
@RequiredArgsConstructor
public class PanoramaController {

    private final PanoramaService panoramaService;

    @GetMapping("/by-location/{locationId}")
    public ApiResponse<List<PanoramaResponse>> findByLocation(@PathVariable UUID locationId) {
        List<PanoramaResponse> data = panoramaService.findByLocationId(locationId).stream()
                .map(PanoramaResponse::from)
                .toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<PanoramaResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(PanoramaResponse.from(panoramaService.findById(id)));
    }
}
