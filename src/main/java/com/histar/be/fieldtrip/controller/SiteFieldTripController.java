package com.histar.be.fieldtrip.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.fieldtrip.dto.FieldTripResponse;
import com.histar.be.fieldtrip.dto.SiteFieldTripScheduleResponse;
import com.histar.be.fieldtrip.dto.SiteTripDecisionRequest;
import com.histar.be.fieldtrip.service.FieldTripLifecycleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-workspaces/{siteCode}/field-trips")
@RequiredArgsConstructor
public class SiteFieldTripController {

    private final FieldTripLifecycleService fieldTripLifecycleService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<List<SiteFieldTripScheduleResponse>> list(@PathVariable String siteCode) {
        return ApiResponse.ok(fieldTripLifecycleService.listForSite(requireUser(), siteCode));
    }

    @PostMapping("/{tripId}/confirm")
    public ApiResponse<FieldTripResponse> confirm(
            @PathVariable String siteCode,
            @PathVariable UUID tripId,
            @Valid @RequestBody SiteTripDecisionRequest request) {
        return ApiResponse.ok(
                fieldTripLifecycleService.confirm(requireUser(), siteCode, tripId, request.rowVersion()));
    }

    @PostMapping("/{tripId}/decline")
    public ApiResponse<FieldTripResponse> decline(
            @PathVariable String siteCode,
            @PathVariable UUID tripId,
            @Valid @RequestBody SiteTripDecisionRequest request) {
        return ApiResponse.ok(fieldTripLifecycleService.decline(
                requireUser(), siteCode, tripId, request.rowVersion(), request.note()));
    }

    private UUID requireUser() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
