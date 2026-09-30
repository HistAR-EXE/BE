package com.histar.be.profile.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.profile.dto.PassportResponse;
import com.histar.be.profile.service.PassportService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MePassportController {

    private final PassportService passportService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/passport")
    public ApiResponse<PassportResponse> passport() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(passportService.getPassport(userId));
    }
}
