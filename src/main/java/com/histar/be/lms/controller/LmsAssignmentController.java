package com.histar.be.lms.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.lms.dto.AssignmentResponse;
import com.histar.be.lms.dto.CreateAssignmentRequest;
import com.histar.be.lms.service.LmsAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lms/assignments")
@RequiredArgsConstructor
public class LmsAssignmentController {

    private final LmsAssignmentService lmsAssignmentService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<AssignmentResponse> create(@RequestBody @Valid CreateAssignmentRequest request) {
        return ApiResponse.ok(lmsAssignmentService.createAssignment(requireUser(), request));
    }

    @GetMapping
    public ApiResponse<List<AssignmentResponse>> list() {
        return ApiResponse.ok(lmsAssignmentService.listForTeacher(requireUser()));
    }

    private UUID requireUser() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }
}
