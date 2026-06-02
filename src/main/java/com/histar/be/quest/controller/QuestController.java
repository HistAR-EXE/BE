package com.histar.be.quest.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.quest.dto.QuestProgressResponse;
import com.histar.be.quest.dto.QuestResponse;
import com.histar.be.quest.service.QuestProgressService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public ApiResponse<PageResponse<QuestResponse>> listQuests(
            @RequestParam(required = false) UUID locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {
        PageRequest pageable = toPageable(page, size, sort);
        var result = questProgressService.listByLocation(locationId, pageable);
        return ApiResponse.ok(toPageResponse(result));
    }

    @GetMapping("/me/quests")
    public ApiResponse<PageResponse<QuestProgressResponse>> myQuests(
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));
        PageRequest pageable = toPageable(page, size, sort);
        var result = questProgressService.listMyQuests(userId, locationId, status, pageable);
        return ApiResponse.ok(toPageResponse(result));
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

    private PageRequest toPageable(int page, int size, String sort) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        String[] sortParts = sort.split(",");
        String sortField = sortParts.length > 0 ? sortParts[0] : "title";
        Sort.Direction direction =
                sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(direction, sortField));
    }

    private <T> PageResponse<T> toPageResponse(org.springframework.data.domain.Page<T> result) {
        return PageResponse.<T>builder()
                .items(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }
}
