package com.histar.be.media.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.media.dto.MediaTranscodeJobResponse;
import com.histar.be.media.service.MediaTranscodeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media/transcode")
@RequiredArgsConstructor
public class MediaTranscodeController {

    private final MediaTranscodeService mediaTranscodeService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<MediaTranscodeJobResponse> enqueue(@RequestPart("file") MultipartFile file) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(mediaTranscodeService.enqueue(userId, file));
    }

    @GetMapping("/{id}")
    public ApiResponse<MediaTranscodeJobResponse> get(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(mediaTranscodeService.get(id, userId));
    }
}
