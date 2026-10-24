package com.histar.be.fieldtrip.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.fieldtrip.dto.CreateFieldTripRequest;
import com.histar.be.fieldtrip.dto.FieldTripResponse;
import com.histar.be.fieldtrip.dto.UpdateFieldTripRequest;
import com.histar.be.fieldtrip.service.FieldTripService;
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

    @PutMapping("/{tripId}")
    public ApiResponse<FieldTripResponse> update(
            @PathVariable UUID tripId, @RequestBody @Valid UpdateFieldTripRequest request) {
        return ApiResponse.ok(
                fieldTripService.update(requireUser(), tripId, request, request.rowVersion()));
    }

    private UUID requireUser() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
