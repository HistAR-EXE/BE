package com.histar.be.photopair.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.photopair.dto.PhotoPairResponse;
import com.histar.be.photopair.service.PhotoPairService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/photo-pairs")
@RequiredArgsConstructor
public class PhotoPairController {

    private final PhotoPairService photoPairService;

    @GetMapping("/by-location/{locationId}")
    public ApiResponse<List<PhotoPairResponse>> findByLocation(@PathVariable UUID locationId) {
        List<PhotoPairResponse> data = photoPairService.findByLocationIdOrderBySortOrder(locationId).stream()
                .map(PhotoPairResponse::from)
                .toList();
        return ApiResponse.ok(data);
    }
}
