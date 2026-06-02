package com.histar.be.quest.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.quest.dto.QuestProgressResponse;
import com.histar.be.quest.dto.QuestResponse;
import com.histar.be.quest.service.QuestProgressService;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuestController {

    private final QuestProgressService questProgressService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/quests")
    public ApiResponse<List<QuestResponse>> listQuests(@RequestParam @NotNull UUID locationId) {
        return ApiResponse.ok(questProgressService.listByLocation(locationId));
    }

    @GetMapping("/me/quests")
    public ApiResponse<List<QuestProgressResponse>> myQuests(@RequestParam @NotNull UUID locationId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(questProgressService.listMyQuests(userId, locationId));
    }

    @PostMapping("/quests/{id}/start")
    public ApiResponse<QuestProgressResponse> startQuest(@PathVariable("id") UUID questId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(questProgressService.startQuest(userId, questId));
    }

    @GetMapping("/quests/{id}/progress")
    public ApiResponse<QuestProgressResponse> questProgress(@PathVariable("id") UUID questId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(questProgressService.getProgress(userId, questId));
    }
}
