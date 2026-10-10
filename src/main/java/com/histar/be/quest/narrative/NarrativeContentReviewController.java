package com.histar.be.quest.narrative;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/narrative-investigations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NarrativeContentReviewController {
    private final NarrativeContentReviewService service;
    private final CurrentUserAccessor currentUser;
    @GetMapping("/{questId}/reviews")
    public ApiResponse<List<NarrativeReviewResponse>> history(@PathVariable UUID questId) { return ApiResponse.ok(service.history(questId)); }
    @PostMapping("/{questId}/reviews")
    public ApiResponse<NarrativeReviewResponse> review(@PathVariable UUID questId, @RequestBody @Valid NarrativeReviewRequest request) {
        String reviewer = currentUser.getEmail().orElseGet(() -> currentUser.getUserId().map(UUID::toString).orElse("unknown"));
        return ApiResponse.ok(service.review(questId, reviewer, request));
    }
}
