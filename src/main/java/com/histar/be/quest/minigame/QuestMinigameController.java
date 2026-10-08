package com.histar.be.quest.minigame;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quests/minigames")
@RequiredArgsConstructor
public class QuestMinigameController {

    private final QuestMinigameService questMinigameService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/{id}")
    public ApiResponse<MinigameViewResponse> open(@PathVariable UUID id) {
        return ApiResponse.ok(questMinigameService.open(currentUser(), id));
    }

    @PostMapping("/{id}/attempts")
    public ApiResponse<MinigameAttemptResponse> submit(@PathVariable UUID id, @RequestBody MinigameAttempt attempt) {
        return ApiResponse.ok(questMinigameService.submit(currentUser(), id, attempt));
    }

    private UUID currentUser() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
