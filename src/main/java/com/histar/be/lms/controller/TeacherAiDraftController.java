package com.histar.be.lms.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.PilotFeatureGate;
import com.histar.be.lms.dto.CreateTeacherAiDraftRequest;
import com.histar.be.lms.dto.TeacherAiDraftResponse;
import com.histar.be.lms.service.TeacherAiDraftService;
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
@RequestMapping("/api/lms/teacher-ai-drafts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherAiDraftController {
    private final TeacherAiDraftService teacherAiDraftService;
    private final CurrentUserAccessor currentUserAccessor;
    private final PilotFeatureGate pilotFeatureGate;

    @PostMapping
    public ApiResponse<TeacherAiDraftResponse> create(@RequestBody @Valid CreateTeacherAiDraftRequest request) {
        pilotFeatureGate.requireTeacherAiDrafts();
        return ApiResponse.ok(teacherAiDraftService.create(requireUser(), request));
    }

    @GetMapping
    public ApiResponse<List<TeacherAiDraftResponse>> list() {
        pilotFeatureGate.requireTeacherAiDrafts();
        return ApiResponse.ok(teacherAiDraftService.list(requireUser()));
    }

    @PutMapping("/{draftId}")
    public ApiResponse<TeacherAiDraftResponse> update(
            @PathVariable UUID draftId, @RequestBody @Valid CreateTeacherAiDraftRequest request) {
        pilotFeatureGate.requireTeacherAiDrafts();
        return ApiResponse.ok(teacherAiDraftService.update(requireUser(), draftId, request));
    }

    @PostMapping("/{draftId}/review")
    public ApiResponse<TeacherAiDraftResponse> review(@PathVariable UUID draftId) {
        pilotFeatureGate.requireTeacherAiDrafts();
        return ApiResponse.ok(teacherAiDraftService.review(requireUser(), draftId));
    }

    @PostMapping("/{draftId}/publish")
    public ApiResponse<TeacherAiDraftResponse> publish(@PathVariable UUID draftId) {
        pilotFeatureGate.requireTeacherAiDrafts();
        return ApiResponse.ok(teacherAiDraftService.publish(requireUser(), draftId));
    }

    private UUID requireUser() {
        return currentUserAccessor.getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }
}
