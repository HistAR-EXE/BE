package com.histar.be.photoscene.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.photoscene.dto.PhotoSceneResponse;
import com.histar.be.photoscene.service.PhotoSceneService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/photo-scenes")
@RequiredArgsConstructor
public class PhotoSceneController {

    private final PhotoSceneService photoSceneService;

    @GetMapping("/by-location/{locationId}")
    public ApiResponse<List<PhotoSceneResponse>> findByLocation(@PathVariable UUID locationId) {
        return ApiResponse.ok(photoSceneService.findByLocationId(locationId));
    }
}
