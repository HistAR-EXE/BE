package com.histar.be.visit.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.visit.dto.EndVisitSessionRequest;
import com.histar.be.visit.dto.StartVisitSessionRequest;
import com.histar.be.visit.dto.VisitSessionResponse;
import com.histar.be.visit.entity.EndReason;
import com.histar.be.visit.service.VisitSessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/visit-sessions")
@RequiredArgsConstructor
public class VisitSessionController {

    private final VisitSessionService visitSessionService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/start")
    public ApiResponse<VisitSessionResponse> start(@RequestBody @Valid StartVisitSessionRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        UUID id = visitSessionService.startSessionWithPersonalization(userId, request);
        return ApiResponse.ok(new VisitSessionResponse(id, request.locationId(), request.mode()));
    }

    @PatchMapping("/{id}/end")
    public ApiResponse<Void> end(@PathVariable UUID id, @RequestBody(required = false) EndVisitSessionRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        EndReason reason = EndReason.USER_EXIT;
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            try {
                reason = EndReason.valueOf(request.reason().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                reason = EndReason.USER_EXIT;
            }
        }
        visitSessionService.endSession(userId, id, reason);
        return ApiResponse.ok(null);
    }
}