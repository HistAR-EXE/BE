package com.histar.be.admin.controller;

import com.histar.be.admin.dto.LocationEraCountResponse;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.location.service.LocationEraValidationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLocationController {

    private final LocationEraValidationService locationEraValidationService;

    @GetMapping("/{id}/era-count")
    public ApiResponse<LocationEraCountResponse> eraCount(@PathVariable UUID id) {
        int count = locationEraValidationService.countDistinctEras(id);
        return ApiResponse.ok(new LocationEraCountResponse(count, count >= 3));
    }
}
