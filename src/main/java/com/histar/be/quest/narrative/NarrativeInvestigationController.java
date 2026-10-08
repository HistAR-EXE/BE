package com.histar.be.quest.narrative;

import com.histar.be.common.response.ApiResponse;
import java.util.UUID;
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

    @GetMapping("/{questId}/investigation")
    public ApiResponse<NarrativeInvestigationResponse> get(@PathVariable UUID questId) {
        return ApiResponse.ok(narrativeInvestigationService.forQuest(questId));
    }
}
