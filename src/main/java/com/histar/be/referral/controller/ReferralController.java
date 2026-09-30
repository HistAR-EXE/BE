package com.histar.be.referral.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.referral.dto.CreatorPublicStats;
import com.histar.be.referral.dto.ReferralLandingResponse;
import com.histar.be.referral.dto.ReferralVisitRequest;
import com.histar.be.referral.service.ReferralService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public creator landing endpoints (no auth required). */
@RestController
@RequestMapping("/api/referral")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/{code}")
    public ApiResponse<ReferralLandingResponse> landing(@PathVariable String code) {
        return ApiResponse.ok(referralService.landing(code));
    }

    @GetMapping("/{code}/stats")
    public ApiResponse<CreatorPublicStats> publicStats(
            @PathVariable String code, @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(referralService.publicStats(code, days));
    }

    @PostMapping("/{code}/visit")
    public ApiResponse<Boolean> visit(
            @PathVariable String code, @RequestBody(required = false) @Valid ReferralVisitRequest request) {
        UUID userId = currentUserAccessor.getUserId().orElse(null);
        String sessionId = request == null ? null : request.sessionId();
        return ApiResponse.ok(referralService.recordVisit(code, userId, sessionId));
    }
}
