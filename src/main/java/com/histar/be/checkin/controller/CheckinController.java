package com.histar.be.checkin.controller;

import com.histar.be.checkin.dto.CheckinRequest;
import com.histar.be.checkin.dto.CheckinResponse;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.gamification.service.GamificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final GamificationService gamificationService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<CheckinResponse> checkin(@RequestBody @Valid CheckinRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        var result = gamificationService.processCheckin(
                userId, request.locationId(), request.latitude(), request.longitude(), request.qrCode());
        return ApiResponse.ok(CheckinResponse.from(result));
    }
}
