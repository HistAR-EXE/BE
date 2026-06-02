package com.histar.be.location.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.location.dto.LocationResponse;
import com.histar.be.location.service.LocationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public ApiResponse<List<LocationResponse>> findAll() {
        List<LocationResponse> data =
                locationService.findAll().stream().map(LocationResponse::from).toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<LocationResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(LocationResponse.from(locationService.findById(id)));
    }
}
