package com.histar.be.photoframe.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.photoframe.dto.PhotoFrameResponse;
import com.histar.be.photoframe.service.PhotoFrameService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/photo-frames")
@RequiredArgsConstructor
public class PhotoFrameController {

    private final PhotoFrameService photoFrameService;

    @GetMapping
    public ApiResponse<List<PhotoFrameResponse>> list() {
        List<PhotoFrameResponse> data = photoFrameService.findAllOrdered().stream()
                .map(PhotoFrameResponse::from)
                .toList();
        return ApiResponse.ok(data);
    }
}
