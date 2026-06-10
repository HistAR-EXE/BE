package com.histar.be.panorama.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.panorama.dto.PanoramaResponse;
import com.histar.be.panorama.service.PanoramaAppService;
import com.histar.be.panorama.service.PanoramaService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/panoramas")
@RequiredArgsConstructor
public class PanoramaController {

    private final PanoramaService panoramaService;
    private final PanoramaAppService panoramaAppService;

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

    /** CP3 Tuần 3: upload ảnh 360 equirectangular lên MinIO + tạo record DB (JWT). */
    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<PanoramaResponse> upload(
            @RequestParam UUID locationId,
            @RequestParam String title,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(panoramaAppService.upload(locationId, title, file));
    }

    @PutMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ApiResponse<PanoramaResponse> replaceImage(
            @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(panoramaAppService.replaceImage(id, file));
    }
}
