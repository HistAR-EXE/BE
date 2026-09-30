package com.histar.be.device.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.device.dto.RegisterPushTokenRequest;
import com.histar.be.device.service.DevicePushTokenService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DevicePushTokenService devicePushTokenService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/push-token")
    public ApiResponse<Void> registerPushToken(@RequestBody @Valid RegisterPushTokenRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        devicePushTokenService.upsert(userId, request);
        return ApiResponse.ok(null);
    }
}
