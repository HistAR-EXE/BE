package com.histar.be.location.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.location.dto.LocationResponse;
import com.histar.be.location.service.LocationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public ApiResponse<PageResponse<LocationResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double nearLat,
            @RequestParam(required = false) Double nearLng,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) String tags) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        String[] sortParts = sort.split(",");
        String sortField = sortParts.length > 0 ? sortParts[0] : "createdAt";
        Sort.Direction direction =
                sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;

        var result = locationService.search(
                city,
                search,
                nearLat,
                nearLng,
                maxDistanceKm,
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(direction, sortField)));
        PageResponse<LocationResponse> body = PageResponse.<LocationResponse>builder()
                .items(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
        return ApiResponse.ok(body);
    }

    @GetMapping("/{id}")
    public ApiResponse<LocationResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(LocationResponse.from(locationService.findById(id)));
    }
}
