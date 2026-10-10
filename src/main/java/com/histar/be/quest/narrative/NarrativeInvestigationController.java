package com.histar.be.quest.narrative;

import com.histar.be.common.response.ApiResponse;
import java.util.UUID;
import jakarta.validation.Valid;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.PilotFeatureGate;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class NarrativeInvestigationController {
    private final NarrativeInvestigationService narrativeInvestigationService;
    private final CurrentUserAccessor currentUserAccessor;
    private final PilotFeatureGate pilotFeatureGate;

    @GetMapping("/{questId}/investigation")
    public ApiResponse<NarrativeInvestigationResponse> get(@PathVariable UUID questId) { enabled();
        return ApiResponse.ok(narrativeInvestigationService.forQuest(questId));
    }
    @GetMapping("/{questId}/investigation/progress")
    public ApiResponse<NarrativeProgressResponse> progress(@PathVariable UUID questId) { enabled(); return ApiResponse.ok(narrativeInvestigationService.progress(user(), questId)); }
    @PostMapping("/{questId}/investigation/actions")
    public ApiResponse<NarrativeProgressResponse> action(@PathVariable UUID questId, @RequestBody @Valid NarrativeActionRequest request) { enabled(); return ApiResponse.ok(narrativeInvestigationService.act(user(), questId, request)); }
    private UUID user(){return currentUserAccessor.getUserId().orElseThrow(()->new AuthException("Unauthorized"));}
    private void enabled(){ pilotFeatureGate.requireNarrativeInvestigation(); }
}
