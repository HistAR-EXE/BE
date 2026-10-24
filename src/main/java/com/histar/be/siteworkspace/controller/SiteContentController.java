package com.histar.be.siteworkspace.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.ContentReviewRequest;
import com.histar.be.siteworkspace.dto.ContentRevisionRequest;
import com.histar.be.siteworkspace.dto.ContentRollbackRequest;
import com.histar.be.siteworkspace.dto.SiteContentDraftRequest;
import com.histar.be.siteworkspace.dto.SiteContentVersionResponse;
import com.histar.be.siteworkspace.entity.SiteContentKind;
import com.histar.be.siteworkspace.service.SiteContentWorkflowService;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-workspaces/{siteCode}/content/{contentKind}/{resourceKey}/versions")
@RequiredArgsConstructor
public class SiteContentController {

    private final SiteContentWorkflowService siteContentWorkflowService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<SiteContentVersionResponse> create(
            @PathVariable String siteCode,
            @PathVariable String contentKind,
            @PathVariable String resourceKey,
            @Valid @RequestBody SiteContentDraftRequest request) {
        return ApiResponse.ok(siteContentWorkflowService.createDraft(
                currentUserId(), siteCode, parseKind(contentKind), resourceKey, request));
    }

    @PatchMapping("/{version}")
    public ApiResponse<SiteContentVersionResponse> update(
            @PathVariable String siteCode,
            @PathVariable String contentKind,
            @PathVariable String resourceKey,
            @PathVariable int version,
            @Valid @RequestBody SiteContentDraftRequest request) {
        return ApiResponse.ok(siteContentWorkflowService.updateDraft(
                currentUserId(),
                siteCode,
                parseKind(contentKind),
                resourceKey,
                version,
                requiredRevision(request.expectedRevision()),
                request));
    }

    @PostMapping("/{version}/submit")
    public ApiResponse<SiteContentVersionResponse> submit(
            @PathVariable String siteCode,
            @PathVariable String contentKind,
            @PathVariable String resourceKey,
            @PathVariable int version,
            @Valid @RequestBody ContentRevisionRequest request) {
        return ApiResponse.ok(siteContentWorkflowService.submitForReview(
                currentUserId(),
                siteCode,
                parseKind(contentKind),
                resourceKey,
                version,
                requiredRevision(request.expectedRevision())));
    }

    @PostMapping("/{version}/review")
    public ApiResponse<SiteContentVersionResponse> review(
            @PathVariable String siteCode,
            @PathVariable String contentKind,
            @PathVariable String resourceKey,
            @PathVariable int version,
            @Valid @RequestBody ContentReviewRequest request) {
        return ApiResponse.ok(siteContentWorkflowService.review(
                currentUserId(),
                siteCode,
                parseKind(contentKind),
                resourceKey,
                version,
                requiredRevision(request.expectedRevision()),
                request.decision(),
                request.note()));
    }

    @PostMapping("/{version}/publish")
    public ApiResponse<SiteContentVersionResponse> publish(
            @PathVariable String siteCode,
            @PathVariable String contentKind,
            @PathVariable String resourceKey,
            @PathVariable int version,
            @Valid @RequestBody ContentRevisionRequest request) {
        return ApiResponse.ok(siteContentWorkflowService.publish(
                currentUserId(),
                siteCode,
                parseKind(contentKind),
                resourceKey,
                version,
                requiredRevision(request.expectedRevision())));
    }

    @PostMapping("/{version}/rollback")
    public ApiResponse<SiteContentVersionResponse> rollback(
            @PathVariable String siteCode,
            @PathVariable String contentKind,
            @PathVariable String resourceKey,
            @PathVariable int version,
            @Valid @RequestBody ContentRollbackRequest request) {
        return ApiResponse.ok(siteContentWorkflowService.rollback(
                currentUserId(),
                siteCode,
                parseKind(contentKind),
                resourceKey,
                version,
                request.reason(),
                requiredRevision(request.expectedRevision())));
    }

    private SiteContentKind parseKind(String contentKind) {
        if (contentKind == null || contentKind.isBlank()) {
            throw new BusinessRuleException("contentKind không hợp lệ");
        }
        try {
            return SiteContentKind.valueOf(contentKind.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("contentKind không hợp lệ");
        }
    }

    private long requiredRevision(Long expectedRevision) {
        if (expectedRevision == null) {
            throw new BusinessRuleException("expectedRevision là bắt buộc");
        }
        return expectedRevision;
    }

    private UUID currentUserId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
