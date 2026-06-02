package com.histar.be.demo.controller;

import com.histar.be.checkin.dto.CheckinResponse;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.DemoProperties;
import com.histar.be.demo.dto.DemoCheckinRequest;
import com.histar.be.gamification.service.GamificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    public static final String DEMO_SECRET_HEADER = "X-Demo-Secret";

    private final DemoProperties demoProperties;
    private final GamificationService gamificationService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/checkin")
    public ApiResponse<CheckinResponse> demoCheckin(
            @RequestHeader(value = DEMO_SECRET_HEADER, required = false) String demoSecret,
            @RequestBody @Valid DemoCheckinRequest request) {
        if (!demoProperties.isEnabled()) {
            throw new ResourceNotFoundException("Not found");
        }
        if (demoProperties.getSecret() == null
                || demoProperties.getSecret().isBlank()
                || demoSecret == null
                || !demoProperties.getSecret().equals(demoSecret)) {
            throw new AuthException("Invalid demo secret");
        }
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        var result = gamificationService.processDemoCheckin(userId, request.locationId());
        return ApiResponse.ok(CheckinResponse.from(result));
    }
}
