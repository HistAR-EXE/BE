package com.histar.be.admin.controller;

import com.histar.be.admin.dto.DemoActivitySnapshot;
import com.histar.be.admin.service.DemoActivityService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.config.DemoProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DemoActivityController {
    private final DemoProperties properties;
    private final DemoActivityService activity;

    @PostMapping("/demo/activity/heartbeat")
    public ApiResponse<DemoActivitySnapshot> heartbeat(Authentication authentication) {
        requireDemo();
        activity.heartbeat(authentication.getName());
        return ApiResponse.ok(activity.snapshot());
    }

    @GetMapping("/internal/demo/activity")
    public ApiResponse<DemoActivitySnapshot> internalStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireDemo();
        String expected = "Bearer " + properties.getControlSecret();
        if (properties.getControlSecret().length() < 32 || authorization == null
                || !MessageDigest.isEqual(
                        authorization.getBytes(StandardCharsets.UTF_8),
                        expected.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessRuleException("Khóa điều khiển demo không hợp lệ");
        }
        return ApiResponse.ok(activity.snapshot());
    }

    private void requireDemo() {
        if (!properties.isEnabled()) {
            throw new BusinessRuleException("Theo dõi hoạt động chỉ khả dụng trong môi trường demo");
        }
    }
}
