package com.histar.be.referral.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.referral.dto.ReferralStatsItem;
import com.histar.be.referral.service.ReferralService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/referral")
@RequiredArgsConstructor
public class AdminReferralController {

    private final ReferralService referralService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ReferralStatsItem>> stats(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(referralService.stats(days));
    }
}
