package com.histar.be.checkin.controller;

import com.histar.be.checkin.dto.CheckinRequest;
import com.histar.be.checkin.dto.CheckinResponse;
import com.histar.be.checkin.dto.CheckinHistoryItem;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.checkin.service.PresenceCheckinService;
import com.histar.be.checkin.service.CheckinHistoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final PresenceCheckinService presenceCheckinService;
    private final CheckinHistoryService checkinHistoryService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<CheckinResponse> checkin(@RequestBody @Valid CheckinRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        var result = presenceCheckinService.checkin(userId, request);
        return ApiResponse.ok(CheckinResponse.from(result));
    }

    @GetMapping("/history")
    public ApiResponse<List<CheckinHistoryItem>> history(@RequestParam UUID locationId) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(checkinHistoryService.history(userId, locationId));
    }
}
