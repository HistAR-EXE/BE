package com.histar.be.fieldtrip.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.fieldtrip.dto.CreateFieldTripRequest;
import com.histar.be.fieldtrip.dto.CreateTripGroupRequest;
import com.histar.be.fieldtrip.dto.FieldTripResponse;
import com.histar.be.fieldtrip.dto.FieldTripTransitionRequest;
import com.histar.be.fieldtrip.dto.UpdateFieldTripRequest;
import com.histar.be.fieldtrip.service.FieldTripJoinService;
import com.histar.be.fieldtrip.service.FieldTripLifecycleService;
import com.histar.be.fieldtrip.service.FieldTripReportingService;
import com.histar.be.fieldtrip.service.FieldTripService;
import com.histar.be.group.dto.GroupSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/field-trips")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherFieldTripController {

    private final FieldTripService fieldTripService;
    private final FieldTripJoinService fieldTripJoinService;
    private final FieldTripLifecycleService fieldTripLifecycleService;
    private final FieldTripReportingService fieldTripReportingService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<FieldTripResponse> create(@RequestBody @Valid CreateFieldTripRequest request) {
        return ApiResponse.ok(fieldTripService.create(requireUser(), request));
    }

    @GetMapping
    public ApiResponse<List<FieldTripResponse>> listMine() {
        return ApiResponse.ok(fieldTripService.listMine(requireUser()));
    }

    @GetMapping("/{tripId}")
    public ApiResponse<FieldTripResponse> get(@PathVariable UUID tripId) {
        return ApiResponse.ok(fieldTripService.get(requireUser(), tripId));
    }

    @PostMapping("/{tripId}/groups")
    public ApiResponse<GroupSummaryResponse> createGroup(
            @PathVariable UUID tripId, @RequestBody @Valid CreateTripGroupRequest request) {
        return ApiResponse.ok(fieldTripJoinService.createGroup(requireUser(), tripId, request));
    }

    @PutMapping("/{tripId}")
    public ApiResponse<FieldTripResponse> update(
            @PathVariable UUID tripId, @RequestBody @Valid UpdateFieldTripRequest request) {
        return ApiResponse.ok(
                fieldTripService.update(requireUser(), tripId, request, request.rowVersion()));
    }

    @PostMapping("/{tripId}/request-confirmation")
    public ApiResponse<FieldTripResponse> requestConfirmation(
            @PathVariable UUID tripId, @RequestBody @Valid FieldTripTransitionRequest request) {
        return ApiResponse.ok(
                fieldTripLifecycleService.requestConfirmation(requireUser(), tripId, request.rowVersion()));
    }

    @PostMapping("/{tripId}/start")
    public ApiResponse<FieldTripResponse> start(
            @PathVariable UUID tripId, @RequestBody @Valid FieldTripTransitionRequest request) {
        return ApiResponse.ok(fieldTripLifecycleService.start(requireUser(), tripId, request.rowVersion()));
    }

    @PostMapping("/{tripId}/complete")
    public ApiResponse<FieldTripResponse> complete(
            @PathVariable UUID tripId, @RequestBody @Valid FieldTripTransitionRequest request) {
        UUID teacherId = requireUser();
        fieldTripReportingService.completeAndSnapshot(teacherId, tripId, request.rowVersion());
        return ApiResponse.ok(fieldTripService.get(teacherId, tripId));
    }

    @PostMapping("/{tripId}/cancel")
    public ApiResponse<FieldTripResponse> cancel(
            @PathVariable UUID tripId, @RequestBody @Valid FieldTripTransitionRequest request) {
        return ApiResponse.ok(fieldTripLifecycleService.cancel(requireUser(), tripId, request.rowVersion()));
    }

    private UUID requireUser() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
