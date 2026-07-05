package com.histar.be.photoframe.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.photoframe.dto.PhotoFrameResponse;
import com.histar.be.photoframe.entity.PhotoFrame;
import com.histar.be.photoframe.service.PhotoFrameAccessService;
import com.histar.be.photoframe.service.PhotoFrameService;
import com.histar.be.profile.service.TierAccessService;
import java.util.ArrayList;
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
    private final PhotoFrameAccessService photoFrameAccessService;
    private final TierAccessService tierAccessService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<List<PhotoFrameResponse>> list() {
        boolean premium = currentUserAccessor
                .getUserId()
                .map(tierAccessService::hasPremiumAccess)
                .orElse(false);
        List<PhotoFrame> frames = photoFrameService.findAllOrdered();
        List<PhotoFrameResponse> data = new ArrayList<>();
        for (int i = 0; i < frames.size(); i++) {
            PhotoFrame frame = frames.get(i);
            boolean free = photoFrameAccessService.isFrameFree(frame, i);
            data.add(PhotoFrameResponse.from(frame, !premium && !free));
        }
        return ApiResponse.ok(data);
    }
}
