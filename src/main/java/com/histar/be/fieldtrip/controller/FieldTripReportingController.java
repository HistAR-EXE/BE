package com.histar.be.fieldtrip.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.fieldtrip.dto.FieldTripLiveBoardResponse;
import com.histar.be.fieldtrip.dto.FieldTripReportResponse;
import com.histar.be.fieldtrip.service.FieldTripReportingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/field-trips")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class FieldTripReportingController {

    private final FieldTripReportingService fieldTripReportingService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/{tripId}/live-board")
    public ApiResponse<FieldTripLiveBoardResponse> liveBoard(@PathVariable UUID tripId) {
        return ApiResponse.ok(fieldTripReportingService.liveBoard(requireUser(), tripId));
    }

    @GetMapping("/{tripId}/report")
    public ApiResponse<FieldTripReportResponse> report(@PathVariable UUID tripId) {
        return ApiResponse.ok(fieldTripReportingService.report(requireUser(), tripId));
    }

    private UUID requireUser() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
