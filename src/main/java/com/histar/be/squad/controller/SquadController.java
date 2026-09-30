package com.histar.be.squad.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.squad.dto.CreateSquadRequest;
import com.histar.be.squad.dto.JoinSquadRequest;
import com.histar.be.squad.dto.SquadCreatedResponse;
import com.histar.be.squad.dto.SquadMeResponse;
import com.histar.be.squad.service.SquadService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<SquadCreatedResponse> create(@RequestBody(required = false) @Valid CreateSquadRequest request) {
        UUID userId = requireUser();
        CreateSquadRequest body = request != null ? request : new CreateSquadRequest(null);
        return ApiResponse.ok(squadService.createSquad(userId, body));
    }

    @PostMapping("/join")
    public ApiResponse<SquadCreatedResponse> join(@RequestBody @Valid JoinSquadRequest request) {
        UUID userId = requireUser();
        return ApiResponse.ok(squadService.joinSquad(userId, request.code()));
    }

    @GetMapping("/me")
    public ApiResponse<SquadMeResponse> me() {
        return ApiResponse.ok(squadService.getMySquad(requireUser()));
    }

    private UUID requireUser() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
