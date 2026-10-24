package com.histar.be.fieldtrip.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.fieldtrip.dto.JoinFieldTripRequest;
import com.histar.be.fieldtrip.dto.TripJoinPreviewResponse;
import com.histar.be.fieldtrip.service.FieldTripJoinService;
import com.histar.be.group.dto.GroupSummaryResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/field-trips")
@RequiredArgsConstructor
public class FieldTripJoinController {

    private final FieldTripJoinService fieldTripJoinService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/join/{code}")
    public ApiResponse<TripJoinPreviewResponse> preview(@PathVariable String code) {
        return ApiResponse.ok(fieldTripJoinService.preview(code));
    }

    @PostMapping("/join")
    public ApiResponse<GroupSummaryResponse> join(@RequestBody @Valid JoinFieldTripRequest request) {
        return ApiResponse.ok(fieldTripJoinService.join(requireUser(), request));
    }

    private UUID requireUser() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
