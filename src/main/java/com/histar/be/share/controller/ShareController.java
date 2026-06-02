package com.histar.be.share.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.config.ViralProperties;
import com.histar.be.share.dto.SharePrefillResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ViralProperties viralProperties;

    @GetMapping("/prefill")
    public ApiResponse<SharePrefillResponse> prefill() {
        return ApiResponse.ok(new SharePrefillResponse(viralProperties.getShareCaption(), new String[] {
            "#TimeLens", "#DiSanVietNam"
        }));
    }
}
