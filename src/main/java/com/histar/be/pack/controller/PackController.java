package com.histar.be.pack.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.pack.dto.PackManifestResponse;
import com.histar.be.pack.service.PackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteCode}/pack")
@RequiredArgsConstructor
public class PackController {

    private final PackService packService;

    @GetMapping
    public ApiResponse<PackManifestResponse> get(
            @PathVariable String siteCode, @RequestParam(defaultValue = "lite") String tier) {
        return ApiResponse.ok(packService.build(siteCode, tier));
    }
}
